package nl.tudelft.distributed.team17.infrastructure;

import nl.tudelft.distributed.team17.application.LedgerExchangeRoundManager;
import nl.tudelft.distributed.team17.application.KnownServerList;
import nl.tudelft.distributed.team17.application.Ledger;
import nl.tudelft.distributed.team17.model.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class InterServerCommunication
{
	private final static Logger LOG = LoggerFactory.getLogger(InterServerCommunication.class);
	private final static long EXCHANGE_LEDGERS_TX_TIMEOUT_MS = 200;

	private ExecutorService executorService;
	private RestTemplate restTemplate;
	private KnownServerList knownServerList;

	private LedgerExchangeRoundManager ledgerExchangeRoundManager;

	@Autowired
	public InterServerCommunication(ExecutorService executorService, RestTemplate restTemplate, KnownServerList knownServerList, LedgerExchangeRoundManager ledgerExchangeRoundManager)
	{
		this.executorService = executorService;
		this.restTemplate = restTemplate;
		this.knownServerList = knownServerList;
		this.ledgerExchangeRoundManager = ledgerExchangeRoundManager;
	}

	private final Object ledgerExchangeLock = new Object();
	public List<Ledger> exchangeLedger(Ledger ledger)
	{
		// Todo: optimization, do not exchange with servers which already exchanged
		synchronized (ledgerExchangeLock)
		{
			LedgerDto ourLedgerAsDto = LedgerDto.from(ledger);

			Set<String> knownServers = knownServerList.getKnownServers();

			// hacky: List of Callable<LedgerDto> instead of Runnable to more easily set timeouts on the tasks through executorService.invokeAll(...)
			List<Callable<LedgerDto>> fns = new ArrayList<>(knownServers.size());

			for(String server : knownServers)
			{
				fns.add(() -> {
					exchangeLedgerWithServer(ourLedgerAsDto, server);
					return null;
				});
			}

			// Ignoring return value as the ledgers are put into the CurrentLedgerExchangeRound indirection layer
			/* List<Future<LedgerDto>> futures = */ executeAsync(fns, EXCHANGE_LEDGERS_TX_TIMEOUT_MS);

			try
			{
				List<Ledger> ledgers =  ledgerExchangeRoundManager.concludeRound(ledger.getGeneration()).stream()
						.map(LedgerDto::toLedger)
						.collect(Collectors.toList());

				return ledgers;
			}
			catch (Exception ex)
			{
				LOG.error("Bugcheck: could not conclude round during exchangeLedger", ex);

				// mem optimization: we know that only one object will be put into this list
				return new ArrayList<>(1);
			}
		}
	}

	public void broadcast(Command command)
	{
		Set<String> knownServers = knownServerList.getKnownServers();
		List<Runnable> fns = new ArrayList<>(knownServers.size());
		for(String server : knownServers)
		{
			// optimization: serialize only once
			executorService.submit(() -> sendCommandToServer(server, command));
		}
	}

	private void sendCommandToServer(String server, Command command)
	{
		URI uriWithLocation = URI.create(server + "/command/");

		try
		{
			restTemplate.postForEntity(uriWithLocation, command, String.class);
		}
		catch (Exception ex)
		{
			LOG.error("Error occurred during broadcast of command to Server [" + server + "]", ex);
		}
	}

	private void exchangeLedgerWithServer(LedgerDto ledgerDto, String server)
	{
		// precache URI's in knownServerList maybe?
		URI uriWithLocation = URI.create(server + "/ledger/");

		// todo: need to record success/failure so that consensus knows how long to wait for other ledgers, or maybe we just exchange Ledgers? How do we fight it out if parallel exchange happens?
		try
		{
			LedgerDto receivedLedgerDto = restTemplate.postForObject(uriWithLocation, ledgerDto, LedgerDto.class);
			ledgerExchangeRoundManager.accept(server, ledgerDto);
		}
		catch (Exception ex)
		{
			LOG.error("Error occurred during Exchange of Ledger with Server [" + server + "], ignoring and continuing", ex);
		}
	}

	private List<Future<LedgerDto>> executeAsync(List<Callable<LedgerDto>> fns, long timeoutInMs)
	{
		try
		{
			return executorService.invokeAll(fns, timeoutInMs, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new RuntimeException(ex);
		}
	}
}