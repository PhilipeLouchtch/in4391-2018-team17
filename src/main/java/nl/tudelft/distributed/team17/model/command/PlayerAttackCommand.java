package nl.tudelft.distributed.team17.model.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import nl.tudelft.distributed.team17.model.Location;
import nl.tudelft.distributed.team17.model.WorldState;

public class PlayerAttackCommand extends PlayerCommand
{
	@JsonProperty("locationToAttack")
	private Location locationToAttack;

	static public PlayerAttackCommand createWithEmailAuthentication(
			String emailAddress,
			Integer clock,
			Location locationToAttack)
	{
		return new PlayerAttackCommand(emailAddress, clock, locationToAttack);
	}

	private PlayerAttackCommand(String emailAddress, Integer clock, Location locationToAttack)
	{
		super(emailAddress, clock, false);
		this.locationToAttack = locationToAttack;
	}

	// Jackson
	private PlayerAttackCommand()
	{
	}

	public Location getLocationToAttack()
	{
		return locationToAttack;
	}

	@Override
	public void apply(WorldState worldState)
	{
		worldState.damageUnit(getPlayerId(), getLocationToAttack());
	}
}