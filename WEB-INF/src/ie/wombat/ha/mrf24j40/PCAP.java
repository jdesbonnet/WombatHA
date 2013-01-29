package ie.wombat.ha.mrf24j40;

public class PCAP {
	// PCAP constants
	public static final int PCAP_MAGIC = 0xa1b2c3d4;
	public static final short PCAP_VERSION_MAJOR = 2;
	public static final short PCAP_VERSION_MINOR = 4;
	public static final int PCAP_TZ = 0;				// thiszone: GMT to local correction
	public static final int PCAP_SIGFIGS = 0;			// sigfigs: accuracy of timestamps
	public static final int PCAP_SNAPLEN = 128;		// snaplen: max len of packets, in octets
	public static final int PCAP_LINKTYPE = 0xc3;		// data link type DLT_IEEE802_15_4 (see <pcap/bpf.h>)
}
