package eqtrade.trading.engine.socket.block;

import eqtrade.trading.core.ITradingEngine;
import eqtrade.trading.core.Utils;
import eqtrade.trading.engine.socket.TradingSocketConnection;

public class LoginRequest extends BaseBlock {
	private int WAITING_VALIDATE = 15000;

	public LoginRequest(String _type, ITradingEngine _engine) {
		super(_type, _engine);
	}

	@Override
	protected Object parsing(Object dt) throws Exception {
		
		//log.info("receive login reply "+dt);

		String st = (String) dt;
		// byte[] bt = (byte[]) dt;
		// String st = new String(Utils.decompress(bt));
		String[] dt2 = st.split("\\|");
		log.info("login receive msg " + st+" "+dt2.length);

		if (dt2.length > 2) {
			String err = dt2[2];
			if (err.contains("error") || err.contains("exception"))
				throw new Exception(err);
		}
		return Long.parseLong(st.split("\\|")[1].trim());
	}
	
	public Object sendAndReceive(String msg) throws Exception {

		Object receive = null;
		// if (issend) {

		log.info("send and receive " + msg);
		synchronized (vreceive) {

			boolean issend = ((TradingSocketConnection) engine.getConnection())
					.send(msg);
			if (issend) {
				vreceive.wait(WAITING_VALIDATE);
				if (vreceive.size() > 0) {
					Object dt = (Object) vreceive.remove(0);
					receive = parsing(dt);
				}else {
					receive = -2l;
				}
			} else {
				log.info("not connected socket " + msg);
			}
		}
		// } else {
		// log.info("not connected socket " + msg);
		// }

		log.info("send and receive finished for " + msg);

		return receive;
	}

}
