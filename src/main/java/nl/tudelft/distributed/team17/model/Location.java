package nl.tudelft.distributed.team17.model;

import java.util.ArrayList;
import java.util.List;

public class Location
{
	private int x;
	private int y;

	static public final Location INVALID_LOCATION = null;

	public Location(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public Location moved(Direction direction)
	{
		int x = this.x, y = this.y;

		switch (direction)
		{
			case up:
				y++;
				break;

			case right:
				x++;
				break;

			case down:
				y--;
				break;

			case left:
				x--;
				break;

			default:
				throw new IllegalArgumentException(String.format("No such direction supported, was: [%s]", direction));
		}

		return new Location(x, y);
	}

	public int maxDistanceTo(Location other)
	{
		return Math.abs(this.x - other.getX()) + Math.abs(this.y - other.getY());
	}

	public List<Direction> getMoveDirectionsTowards(Location targetLocation)
	{
		List<Direction> moveDirections = new ArrayList<>();
		Integer deltaX = getX() - targetLocation.getX();
		Integer deltaY = getY() - targetLocation.getY();
		if(deltaX > 0)
		{
			moveDirections.add(Direction.down);
		}
		else if(deltaX < 0)
		{
			moveDirections.add(Direction.up);
		}
		if(deltaY > 0)
		{
			moveDirections.add(Direction.left);
		}
		else if(deltaY < 0)
		{
			moveDirections.add(Direction.right);
		}
		return moveDirections;
	}

	@Override
	public String toString()
	{
		return "Location{" +
				"x=" + x +
				", y=" + y +
				'}';
	}
}
