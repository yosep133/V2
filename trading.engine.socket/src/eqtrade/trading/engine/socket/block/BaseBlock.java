package eqtrade.trading.engine.socket.block;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eqtrade.trading.core.ITradingEngine;
import eqtrade.trading.engine.TradingConnection;
import eqtrade.trading.engine.socket.TradingSocketConnection;

public class BaseBlock {
	protected ITradingEngine engine;
	protected Vector vreceive = new Vector();
	protected Log log = LogFactory.getLog(getClass());
	protected String type;

	public BaseBlock(String _type, ITradingEngine _engine) {
		this.engine = _engine;
		this.type = _type;
	}

	public Object sendAndReceive(String msg) throws Exception {

		Object receive = "";
		// if (issend) {

		//log.info("send and receive " + msg);
		synchronized (vreceive) {

			boolean issend = ((TradingSocketConnection) engine.getConnection())
					.send(msg);
			if (issend) {
				vreceive.wait();
				if (vreceive.size() > 0) {
					Object dt = (Object) vreceive.remove(0);
					receive = parsing(dt);
				}
			} else {
				log.info("not connected socket " + msg);
			}
		}
		// } else {
		// log.info("not connected socket " + msg);
		// }

		//log.info("send and receive finished for " + msg);

		return receive;
	}

	public Object sendAndReceive() throws Exception {
		return sendAndReceive(engine.getConnection().getSessionid() + "|"
				+ type);
	}

	public Object sendAndReceiveWithParam(String... param) throws Exception {
		String ss = engine.getConnection().getSessionid() + "|" + type + "|";
		for(int i=0;i<param.length;i++){
			ss+= param[i];
			if(i<(param.length-1))
				ss+="|";
		}

		return sendAndReceive(ss);

	}

	protected Object parsing(Object dt) throws Exception {
		String rec = (String) dt;
		rec = rec.substring(rec.indexOf("|") + 1, rec.length());
		return rec;
	}

	public void addReceive(Object msg) {
		// log.info("receive " + msg);

		synchronized (vreceive) {
			vreceive.add(msg);
			vreceive.notify();
		}
	}

	public void clear() {
		synchronized (vreceive) {
			vreceive.clear();
			vreceive.notify();
		}
	}

}
