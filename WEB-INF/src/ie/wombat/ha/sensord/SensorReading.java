package ie.wombat.ha.sensord;

class SensorReading {
	public SensorReading (int timestamp, String sensorId, String sensorType, float value) {
		this.timestamp = timestamp;
		this.sensorId = sensorId;
		this.sensorType = sensorType;
		this.value = value;
	}
	public String sensorId;
	public String sensorType;
	public int sensorNumber;
	public float value;
	public int timestamp;
	
	public String toString() {
		return timestamp + " " + sensorType + " " + sensorId + " " + value;
	}
}