package eqtrade.trading.engine;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Message;

import eqtrade.trading.core.Event;
import eqtrade.trading.core.ITradingEngine;
import eqtrade.trading.core.Utils;

public class ThreadSocketOut extends ThreadOut {
	private Logger log = LoggerFactory.getLogger(getClass());

	public ThreadSocketOut(ITradingEngine engine) {
		super(engine);
	}

	@Override
	public void processOut(Vector vdatamsg) {
		boolean success = false;
		int i = 0;
		try { 
			for (; i < vdatamsg.size(); i++) {
				Event evt = (Event) vdatamsg.elementAt(i);
				if (engine.getConnection().getSessionid() > -1) {
					String event = evt.getName();
					if (event.equals(TradingEventDispatcher.EVENT_DELETEORDER)) {
					} else if (event.equals(TradingEventDispatcher.EVENT_REQUESTWITHDRAW)) {
						String msg = engine.getConnection().getSessionid()
								+ "|"
								+ ((Message) evt
										.getField(TradingEventDispatcher.PARAM_FIX))
										.toString().replace("\001", "|");
						success = engine.getConnection().entryWithdraw(
								Utils.compress(msg.getBytes()));
					} else if (event
							.equals(TradingEventDispatcher.EVENT_REQUESTAMEND)) {
						String msg = engine.getConnection().getSessionid()
								+ "|"
								+ ((Message) evt
										.getField(TradingEventDispatcher.PARAM_FIX))
										.toString().replace("\001", "|");
						success = engine.getConnection().entryAmend(
								Utils.compress(msg.getBytes()));
					} else if (event
							.equals(TradingEventDispatcher.EVENT_CREATESCHEDULE)) {						
						String msg = engine.getConnection().getSessionid()
								+ "|"
								+ ((Message) evt
										.getField(TradingEventDispatcher.PARAM_FIX))
										.toString().replace("\001", "|");
						success = engine.getConnection().createAts(
								Utils.compress(msg.getBytes()));
						
						//success = engine
							//	.getConnection()
								//.createAts(
									//	Utils.compress(((Message) evt
										//		.getField(TradingEventDispatcher.PARAM_FIX))
											//	.toString().getBytes()));
					} else if (event
							.equals(TradingEventDispatcher.EVENT_WITHDRAWATS)) {
						
						String msg = engine.getConnection().getSessionid()
								+ "|"
								+ ((Message) evt
										.getField(TradingEventDispatcher.PARAM_FIX))
										.toString().replace("\001", "|");
						success = engine.getConnection().atsWithdraw(
								Utils.compress(msg.getBytes()));
						
						//success = engine
							//	.getConnection()
								//.atsWithdraw(
									//	Utils.compress(((Message) evt
										//		.getField(TradingEventDispatcher.PARAM_FIX))
											//	.toString().getBytes()));
					} else if (event
							.equals(TradingEventDispatcher.EVENT_CREATEORDER)) {
						log.info(""+evt.getParam());
						String msg = engine.getConnection().getSessionid()
								+ "|"
								+ ((Message) evt
										.getField(TradingEventDispatcher.PARAM_FIX))
										.toString().replace("\001", "|");
						success = engine.getConnection().createOrder(
								Utils.compress(msg.getBytes()));
					} else if (event
							.equals(TradingEventDispatcher.EVENT_NOTIFICATION)) {
						
						String msg = engine.getConnection().getSessionid()
								+ "|"
								+ ((Message) evt
										.getField(TradingEventDispatcher.PARAM_FIX))
										.toString().replace("\001", "|");
						success = engine.getConnection().notification(
								Utils.compress(msg.getBytes()));
						
					} else if (event.equals(TradingEventDispatcher.EVENT_CREATEORDERMATRIX)) {
						//System.out.println("ThreadSocketOut EVENT_CREATEORDERMATRIX");
						String msg = engine.getConnection().getSessionid() + "|" + ((Message) evt.getField(TradingEventDispatcher.PARAM_FIX)).toString().replace("\001", "|");
						success = engine.getConnection().createOrderMatrix(Utils.compress(msg.getBytes()));
                        //System.out.println("IS SUCCESS EVENT_CREATEORDERMATRIX : " + success);
					} else if (event.equals(TradingEventDispatcher.EVENT_DELETEORDERMATRIX)) {						
						//System.out.println("ThreadSocketOut EVENT_DELETEORDERMATRIX");
						String msg = engine.getConnection().getSessionid() + "|" + ((Message) evt.getField(TradingEventDispatcher.PARAM_FIX)).toString().replace("\001", "|");
						success = engine.getConnection().deleteOrderMatrix(Utils.compress(msg.getBytes()));
						//System.out.println("IS SUCCESS EVENT_DELETEORDERMATRIX : " + success);
                             
					} else if (event.equals(TradingEventDispatcher.EVENT_DELETEORDERMATRIXDETIL)) {
						//System.out.println("ThreadSocketOut EVENT_DELETEORDERMATRIXDETIL");
						String msg = engine.getConnection().getSessionid() + "|" + ((Message) evt.getField(TradingEventDispatcher.PARAM_FIX)).toString().replace("\001", "|");
						success = engine.getConnection().deleteOrderMatrix(Utils.compress(msg.getBytes()));  
					} else if (event//*gtc
							.equals(TradingEventDispatcher.EVENT_CREATEGTC)) {
						String msg = engine.getConnection().getSessionid()
								+ "|"
								+ ((Message) evt
										.getField(TradingEventDispatcher.PARAM_FIX))
										.toString().replace("\001", "|");
						success = engine.getConnection().createGtc(
								Utils.compress(msg.getBytes()));
						
					} else if (event
							.equals(TradingEventDispatcher.EVENT_WITHDRAWGTC)) {
						
						String msg = engine.getConnection().getSessionid()
								+ "|"
								+ ((Message) evt
										.getField(TradingEventDispatcher.PARAM_FIX))
										.toString().replace("\001", "|");
						success = engine.getConnection().createGtc(
								Utils.compress(msg.getBytes()));	
						
					} else if (event
							.equals(TradingEventDispatcher.EVENT_AMMENDGTC)) {
						
						String msg = engine.getConnection().getSessionid()
								+ "|"
								+ ((Message) evt
										.getField(TradingEventDispatcher.PARAM_FIX)
										//.getField(TradingEventDispatcher.PARAM_NEWORDER)
										)
										.toString().replace("\001", "|");
						success = engine.getConnection().createGtc(
								Utils.compress(msg.getBytes()));
						
                      //  }rmi.createOrderMatrix(engine.getConnection().sesid, Utils.compress(((Message)evt.getField(TradingEventDispatcher.PARAM_FIX)).toString().getBytes()));                        
                    	
						//success = engine
							//	.getConnection()
								//.notification(
									//	((Message) evt
										//		.getField(TradingEventDispatcher.PARAM_FIX))
											//	.toString());
						// success =
						// engine.getConnection().rmi.send_fixmsg(engine.getConnection().sesid,
						// Utils.compress(((Message)evt.getField(TradingEventDispatcher.PARAM_FIX)).toString().getBytes()));
						//System.out.println("IS SUCCESS EVENT_DELETEORDERMATRIXDETIL : " + success);
                      //  rmi.createOrderMatrix(engine.getConnection().sesid, Utils.compress(((Message)evt.getField(TradingEventDispatcher.PARAM_FIX)).toString().getBytes()));                        
						//success = engine
							//	.getConnection()
								//.notification(
									//	((Message) evt
										//		.getField(TradingEventDispatcher.PARAM_FIX))
											//	.toString());
						// success =
						// engine.getConnection().rmi.send_fixmsg(engine.getConnection().sesid,
						// Utils.compress(((Message)evt.getField(TradingEventDispatcher.PARAM_FIX)).toString().getBytes()));
					} else {
						// success =
						// engine.getConnection().rmi.send_fixmsg(engine.getConnection().sesid,
						// Utils.compress(((Message)evt.getField(TradingEventDispatcher.PARAM_FIX)).toString().getBytes()));
					}
					// if (!success){
					// if (evt.isNeedACK()){
					// HashMap param = evt.getParam();
					// param.put(TradingEventDispatcher.PARAM_RESULT, new
					// Boolean(false));
					// engine.getEventDispatcher().pushEvent(new
					// Event(evt.getName()+".nack", param));
					// }
					// throw new
					// Exception("send message to server failed, result from server is: "+success);
					// }
					// if (evt.isNeedACK()){
					// HashMap param = evt.getParam();
					// param.put(TradingEventDispatcher.PARAM_RESULT, new
					// Boolean(success));
					// engine.getEventDispatcher().pushEvent(new
					// Event(evt.getName()+".ack", param));
					// }
					// try {Thread.sleep(10);} catch (Exception ex){}
				} else {
					int from = i, to = vdatamsg.size();
					for (int k = from; k < to; k++) {
						Event evt2 = (Event) vdatamsg.elementAt(k);
						if (!evt2.getName().equals(CMD_REQUEST)) {
							if (evt2.isNeedACK()) {
								HashMap param = evt2.getParam();
								param.put(TradingEventDispatcher.PARAM_RESULT,
										new Boolean(false));
								engine.getEventDispatcher().pushEvent(
										new Event(evt2.getName() + ".nack",
												param));
							}
						}
					}
					break;
				}
			}
		} catch (RemoteException ex) {
			// kembalikan dahulu ke status semula
			int from = i, to = vdatamsg.size();
			for (int k = from; k < to; k++) {
				Event evt2 = (Event) vdatamsg.elementAt(k);
				if (!evt2.getName().equals(CMD_REQUEST)) {
					if (evt2.isNeedACK()
							&& !evt2.getName().endsWith(".callback")) {
						HashMap param = evt2.getParam();
						param.put(TradingEventDispatcher.PARAM_RESULT,
								new Boolean(false));
						engine.getEventDispatcher().pushEvent(
								new Event(evt2.getName() + ".nack", param));
					}
				}
			}
			//log.error(Utils.logException(ex));
			engine.getConnection().reconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
			// kembalikan dahulu ke status semula
			int from = i, to = vdatamsg.size();
			for (int k = from; k < to; k++) {
				Event evt2 = (Event) vdatamsg.elementAt(k);
				if (!evt2.getName().equals(CMD_REQUEST)) {
					if (evt2.isNeedACK()
							&& !evt2.getName().endsWith(".callback")) {
						HashMap param = evt2.getParam();
						param.put(TradingEventDispatcher.PARAM_RESULT,
								new Boolean(false));
						engine.getEventDispatcher().pushEvent(
								new Event(evt2.getName() + ".nack", param));
					}
				}
			}
			//log.error(Utils.logException(ex));
			engine.getConnection().reconnect();
		}
	}
}
