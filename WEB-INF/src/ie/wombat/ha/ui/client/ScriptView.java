package ie.wombat.ha.ui.client;


/**
 * Scripting.
 */
public class ScriptView extends CenterAreaView  {

	public ScriptView() {
		super(false);		
		setViewTitle("Script View");
		vp.add(new ScriptPanel());
	}
	
}
