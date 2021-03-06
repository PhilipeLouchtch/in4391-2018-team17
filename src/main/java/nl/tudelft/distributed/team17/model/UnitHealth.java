package nl.tudelft.distributed.team17.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rits.cloning.Immutable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@Immutable
@JsonAutoDetect(isGetterVisibility = NONE, fieldVisibility = NONE, getterVisibility = NONE, setterVisibility = NONE)
public class UnitHealth
{
	@JsonProperty("current")
	private int current;

	@JsonProperty("maximum")
	private int maximum;

	public UnitHealth(int current, int maximum)
	{
		if (current < 0)
		{
			throw new RuntimeException("Unit health cannot be lower than zero");
		}

		this.current = current;
		this.maximum = maximum;
	}

	@JsonCreator
	private UnitHealth()
	{
	}

	public UnitHealth damaged(int damage)
	{
		int newHealth = Math.max(current - damage, 0);
		return new UnitHealth(newHealth, maximum);
	}

	public UnitHealth healed(int heal)
	{
		int newHealth = Math.min(current + heal, maximum);
		return new UnitHealth(newHealth, maximum);
	}

	public boolean isEmpty()
	{
		return current == 0;
	}

	public boolean halfHealthOrLess()
	{
		return current/maximum <= 1/2;
	}

	public byte[] getHash()
	{
		MessageDigest messageDigest = new DigestUtils(MessageDigestAlgorithms.SHA_256).getMessageDigest();
		messageDigest = DigestUtils.updateDigest(messageDigest, ByteBuffer.allocate(4).putInt(current));
		messageDigest = DigestUtils.updateDigest(messageDigest, ByteBuffer.allocate(4).putInt(maximum));

		return messageDigest.digest();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		UnitHealth that = (UnitHealth) o;
		return current == that.current &&
				maximum == that.maximum;
	}
}
