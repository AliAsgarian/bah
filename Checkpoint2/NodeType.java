import absyn.*;

/* what the name is and what the type is, and the level */
public class NodeType {
	public String name;
	public Dec def;
	public int level;

	public NodeType(String name, Dec def, int level) {
		this.name = name;
		this.def = def;
		this.level = level;
	}
}