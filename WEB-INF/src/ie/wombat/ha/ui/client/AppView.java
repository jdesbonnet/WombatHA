package ie.wombat.ha.ui.client;

/**
 * Display apps
 */
public class AppView extends CenterAreaView {

	public AppView() {
		super(false);
		setViewTitle("Apps View");
		vp.add(new TimeChart(AppFrame.INSTANCE.getNetworkId(),800, 350));
	}
}
