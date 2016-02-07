package cluster;

public class Node {

	private final int id;
	private final double dist_value;

	private Node left;
	private Node right;

	public Node(int id, double dist_value) {

		this.id = id;
		this.dist_value = dist_value;
	}

	public void setLeftChild(Node child) {

		this.left = child;
	}

	public void setRightChild(Node child) {

		this.right = child;
	}

	public int getId() {

		return id;
	}

	public double getDistValue() {

		return dist_value;
	}

	public Node getLeftChild() {

		return left;
	}

	public Node getRightChild() {

		return right;
	}

	@Override
	public String toString() {

		return "NODE" + id + "X";
	}
}
