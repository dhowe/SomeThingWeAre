package stwa;

public class UIEvent {
	
	private String command, duration;
	private Object data = "";
	private float position;

	public UIEvent() {}
	
	public UIEvent(String cmd) {
		this.command = cmd;
	}
	
	public UIEvent(String cmd, Object data) {
		this.setCommand(cmd);
		this.setData(data);
	}

	public String toString() {
		return toJSON();
	}
	
	public String toJSON() {
		return "{ command: " + command + ", position: " + position + 
				", duration: " + duration + ", data: " + getData() + " }";
	}
	
	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public float getPosition() {
		return position;
	}
	
	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}
	public void setPosition(float position) {
		this.position = position;
	}

}
