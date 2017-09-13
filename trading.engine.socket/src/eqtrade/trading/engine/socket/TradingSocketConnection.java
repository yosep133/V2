package eqtrade.trading.engine.socket;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.eqtrade.ClientSocket;
import com.eqtrade.Receiver;
import com.eqtrade.SocketFactory;
import com.eqtrade.SocketInterface;
import com.vollux.util.Util;

import eqtrade.trading.core.Event;
import eqtrade.trading.core.ITradingConnection;
import eqtrade.trading.core.ITradingEngine;
import eqtrade.trading.core.Utils;
import eqtrade.trading.engine.ThreadIn;
import eqtrade.trading.engine.ThreadOut;
import eqtrade.trading.engine.ThreadSocketOut;
import eqtrade.trading.engine.TradingEventDispatcher;
import eqtrade.trading.engine.TradingStore;
import eqtrade.trading.model.Order;
import eqtrade.trading.model.OrderMatrix;
import eqtrade.trading.model.UserProfile;

public class TradingSocketConnection implements ITradingConnection, Receiver {
	private ITradingEngine engine;
	private String uid, pwd, pin;
	private String ssvr;
	private int totalServer, ninterval;
	private HashMap hashIP = new HashMap();
	private Log log = LogFactory.getLog(getClass());

	private BlockRequestSynchronized blockingRequest;
	private ThreadIn inThread;
	private ThreadOut outThread;
	private Timer refreshTimer = null;
	private final static String CMD_REQUEST = "RQT";
	public volatile long sesid = 0;
	private SocketInterface socketConnector;
	private boolean isLogout = false, isValidated = false;
	private String sfilterClient = null;
//	java.util.Timer timerServer;
//	ReqTime reqTimeServer ;
	private Log logFile = LogFactory.getLog("TradingConnectionSocketFile");


	public TradingSocketConnection(ITradingEngine engine) {
		this.engine = engine;
		blockingRequest = new BlockRequestSynchronized(engine);
		init();
	}

	public void init() {
		try {
			loadSetting();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		inThread = new ThreadIn(engine);
		outThread = new ThreadSocketOut(engine);
		refreshTimer = new Timer(1000 * 10, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Event evt = new Event(CMD_REQUEST, null);
				// processHeartbeat(evt);
				// log.info("execute action listeners");
				(new Thread(new Runnable() {
					@Override
					public void run() {
						Event evt = new Event(CMD_REQUEST, null);
						processHeartbeat(evt);
					}
				})).start();
			}
		});
		refreshTimer.setInitialDelay(1000 * 10);
		refreshTimer.stop();
		/*if(reqTimeServer != null) {
			reqTimeServer.stop();
			reqTimeServer = null;
		}
		reqTimeServer = new ReqTime(this);
		reqTimeServer.start();*/
	}

	private void processHeartbeat(Event evt) {
		byte[] bpar = null;
		try {
			if (sesid > -1) {
				if (evt.getName().equals(CMD_REQUEST)) {
					refreshTimer.stop();
					// log.info("process heartbeat " + sesid);
					send(getSessionid() + "|heartbeat");
					// bpar = rmi.getCurrentMsg(sesid);

					// log.info("process heartbeat finished");
					/*
					 * if (bpar != null) { String temp = new
					 * String(Utils.decompress(bpar)); JSONArray array =
					 * (JSONArray) JSONValue.parse(temp); Vector vintmp = new
					 * Vector(array.size()); for (int i = 0; i < array.size();
					 * i++) { log.info("server say: " +
					 * array.get(i).toString());
					 * vintmp.addElement(array.get(i)); }
					 * inThread.addIn(vintmp); }
					 * 
					 * if (process) refreshTimer.start();
					 */
					refreshTimer.start();
				}
			}
		} catch (Exception ex) {
			//log.error(Utils.logException(ex));
			reconnect();
			/*
			 * logout(); engine.getEventDispatcher() .pushEvent( new
			 * Event(TradingEventDispatcher.EVENT_KILL) .setField(
			 * TradingEventDispatcher.PARAM_MESSAGE,
			 * "Force logout because you have been login at another computer or bad connection"
			 * ));
			 */}
	}

	private void loadSetting() throws Exception {
		System.out.println("---");
		socketConnector = SocketFactory.createSocket(
				"data/config/socket.ini", this);

		Object o = Utils.readFile("data/config/trading.dat");
		Vector v = null;

		if (o == null) {
			v = new Vector(3);
			v.addElement("127.0.0.1:8080");
			v.addElement("127.0.0.1:8080");
			// v.addElement("192.168.102.10:7899/Gateway");
			// v.addElement("192.168.102.10:7899/Gateway");
			// v.addElement(new Integer(600));
			// v.add("http://forex.simasnet.com");
			Utils.writeFile("data/config/trading.dat", v);
		} else {
			v = (Vector) o;
		}
		// log.info("v elememt " + v.elementAt(2) + " " + v.size());
		// ninterval = ((Integer) v.elementAt(2)).intValue();
		// ninterval = 300;
		int x = v.size();
		// ninterval = ((Integer) v.elementAt(x-1)).intValue();

		totalServer = x - 1;
		hashIP.clear();
		for (int i = 0; i <= totalServer; i++) {
			//log.info("ssvr " + v.elementAt(i));
			ssvr = (String) v.elementAt(i);
			hashIP.put(new String(i + ""), new String(ssvr));
		}
		// try {
		// sip = InetAddress.getLocalHost().getHostAddress();
		// } catch (Exception ex){}
		//log.info("available server: " + totalServer);
		//log.info("available server: " + hashIP.toString());
	}

	@Override
	public void changePIN(final String soldpin, final String snewpin) {
		new Thread(new Runnable() {
			public void run() {
				try {
					String rec = (String) blockingRequest.get(
							BlockRequestSynchronized.REQUEST_CHANGEPIN)
							.sendAndReceiveWithParam(
									uid.toUpperCase() + "|" + snewpin);// rmi.changepin(sesid,
																		// uid,
					// snewpin);
					boolean success = rec.equals("ok") ? true : false;
					if (success) {
						engine.getEventDispatcher()
								.pushEvent(
										new Event(
												TradingEventDispatcher.EVENT_CHGPINOK));
						pin = snewpin;
					} else {
						engine.getEventDispatcher()
								.pushEvent(
										new Event(
												TradingEventDispatcher.EVENT_CHGPINFAILED)
												.setField(
														TradingEventDispatcher.PARAM_MESSAGE,
														"change PIN failed, please try again."));
					}
				} catch (Exception ex) {
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_CHGPINFAILED)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"change PIN failed, please check your connection"));
				}
			}
		}).start();
	}

	@Override
	public void changePassword(final String soldpwd, final String snewpwd) {
		new Thread(new Runnable() {
			public void run() {
				try {
					String ok = (String) blockingRequest.get(
							BlockRequestSynchronized.REQUEST_CHANGEPASSWORD)
							.sendAndReceiveWithParam(
									uid.toUpperCase() + "|" + snewpwd);
					// yosep enforcementpassword
//					String[] result = ok.split("#");
					boolean success = ok.equals("ok");
//					if (result[0].equalsIgnoreCase("Y")) {
						if (success) {
							engine.getEventDispatcher()
									.pushEvent(
											new Event(
													TradingEventDispatcher.EVENT_CHGPASSWDOK));
						} else {
							engine.getEventDispatcher()
									.pushEvent(
											new Event(
													TradingEventDispatcher.EVENT_CHGPASSWDFAILED)
													.setField(
															TradingEventDispatcher.PARAM_MESSAGE,
															"change password failed, please try again."));
//															result[1]));
						}
//					}
				} catch (Exception ex) {
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_CHGPASSWDFAILED)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"change password failed, please check your connection"));
				}
			}
		}).start();
	}

	@Override
	public void checkPIN(final String puserid, final String pPIN) {
		new Thread(new Runnable() {
			public void run() {
				try {
					String ok = (String) blockingRequest.get(
							BlockRequestSynchronized.REQUEST_CHECKPIN)
							.sendAndReceiveWithParam(
									puserid.toUpperCase() + "|" + pPIN);
					boolean success = ok.equals("ok");
					if (success) {
						engine.getEventDispatcher()
								.pushEvent(
										new Event(
												TradingEventDispatcher.EVENT_CHECKPINOK));
						pin = pPIN;
					} else {
						engine.getEventDispatcher()
								.pushEvent(
										new Event(
												TradingEventDispatcher.EVENT_CHECKPINFAILED)
												.setField(
														TradingEventDispatcher.PARAM_MESSAGE,
														"invalid PIN, please try again."));
					}
				} catch (Exception ex) {
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_CHECKPINFAILED)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"logon trading failed, please check your connection"));
				}
			}
		}).start();
	}
	
	@Override
	public void checkPIN_NEGO(final String pPIN, final String form) {
		new Thread(new Runnable() {
			public void run() {
				try {
					String ok = (String) blockingRequest.get(
							BlockRequestSynchronized.REQUEST_CHECKPIN_NEGO)
							.sendAndReceiveWithParam(pPIN);
					boolean success = ok.equals("ok");
					System.out.println("OK GA YA"+ok);
					if (success) {
						engine.getEventDispatcher()
								.pushEvent(
										new Event(
												TradingEventDispatcher.EVENT_CHECKPIN_NEGO_OK)
										.setField(TradingEventDispatcher.PARAM_FORM, form));
												;
						pin = pPIN;
					} else {
						engine.getEventDispatcher()
								.pushEvent(
										new Event(
												TradingEventDispatcher.EVENT_CHECKPIN_NEGO_FAILED)
												.setField(
														TradingEventDispatcher.PARAM_MESSAGE,
														"invalid PIN Nego, please try again.")
												.setField(TradingEventDispatcher.PARAM_FORM, form));
					}
				} catch (Exception ex) {
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_CHECKPIN_NEGO_FAILED)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"login PIN Nego failed, please check your connection"));
				}
			}
		}).start();
	}

	@Override
	public void checkPINSLS(final String puserid, final String pPIN) {
		new Thread(new Runnable() {
			public void run() {
				try {
					String ok = (String) blockingRequest.get(
							BlockRequestSynchronized.REQUEST_CHECKPIN)
							.sendAndReceiveWithParam(
									puserid.toUpperCase() + "|" + pPIN);
					boolean success = ok.equals("ok");
					if (success) {
						engine.getEventDispatcher()
								.pushEvent(
										new Event(
												TradingEventDispatcher.EVENT_CHECKPINOK));
						pin = pPIN;
					}
				} catch (Exception ex) {
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_CHECKPINFAILED)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"logon trading failed, please check your connection"));
				}
			}
		}).start();
	}

	@Override
	public boolean createOrder(Object data) throws Exception {

		// channel.write((byte[]) data).awaitUninterruptibly();
		socketConnector.sendMessage(data);

		return true;
	}

	@Override
	public boolean entryAmend(Object data) throws Exception {
		// channel.write((byte[]) data).awaitUninterruptibly();
		socketConnector.sendMessage(data);

		return true;
	}

	@Override
	public boolean entryWithdraw(Object data) throws Exception {
		socketConnector.sendMessage(data);

		return true;
	}

	@Override
	public String getIPServer() {
		return "";
	}

	@Override
	public String getPIN() {
		return pin;
	}

	@Override
	public String getPassword() {
		return pwd;
	}

	@Override
	public void getServertime() throws Exception {
		engine.getEventDispatcher().pushEvent(
				new Event(TradingEventDispatcher.EVENT_TIMESERVER).setField(
						TradingEventDispatcher.PARAM_TIMESERVER,
						(Long) blockingRequest.get(
								BlockRequestSynchronized.REQUEST_TIME)
								.sendAndReceive()));
	}

	@Override
	public String getUserId() {
		return uid;
	}

	@Override
	public boolean isConnected() {
		return socketConnector.isConnected();

	}

	@Override
	public void loadAccount(String eventStatus, String param1, boolean clear)
			throws Exception {
		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAACCOUNT,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading account...")
								.setField(TradingEventDispatcher.PARAM_CLEAR,
										new Boolean(clear))
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATAACCOUNT)
												.sendAndReceiveWithParam(param1))
												.getBytes())));
	}

	@Override
	public void loadCashColl(String eventStatus, String param1, boolean clear)
			throws Exception {
		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATACASHCOLL,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading cash collateral...")
								.setField(TradingEventDispatcher.PARAM_CLEAR,
										new Boolean(clear))
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATACASHCOLL)
												.sendAndReceiveWithParam(param1))
												.getBytes())));
	}

	@Override
	public void loadMaster(String eventStatus) throws Exception {
		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAACCOUNTTYPE,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(5%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATAACCTYPE)
												.sendAndReceive()).getBytes())));

//*gtc awal
		engine.getEventDispatcher()
		.pushEvent(
				new Event(TradingEventDispatcher.EVENT_DATAGTC,true)
						.setField(TradingEventDispatcher.PARAM_CODE,
						eventStatus)
						.setField(TradingEventDispatcher.PARAM_MESSAGE,
								"loading...")
						.setField(
								TradingEventDispatcher.PARAM_DATA,
								Utils.compress(((String) blockingRequest
										.get(BlockRequestSynchronized.REQUEST_DATAGTC)
										.sendAndReceive() )
										.getBytes())));
//*gtc akhir
		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAINVTYPE,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(15%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATAINVTYPE)
												.sendAndReceive()).getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAUSERTYPE,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(25%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATAUSERTYPE)
												.sendAndReceive()).getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATACUSTTYPE,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(35%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATACUSTTYPE)
												.sendAndReceive()).getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAEXCHANGE,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(45%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATAEXCHANGE)
												.sendAndReceive()).getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATABOARD, true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(55%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATABOARD)
												.sendAndReceive()).getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATABROKER, true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(60%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATABROKER)
												.sendAndReceive()).getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAACCSTATUS,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(65%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATASTATUSACC)
												.sendAndReceive()).getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAORDERSTATUS,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(75%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATASTATUSORDER)
												.sendAndReceive()).getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAACCSTOCK,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(85%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATAACCTYPESEC)
												.sendAndReceive()).getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATASTOCK, true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(95%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATASECURITITES)
												.sendAndReceive()).getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAUSERPROFILE,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(98%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATAUSERPROFILE)
												.sendAndReceive(
														sesid + "|userprofile|"
																+ uid))
												.getBytes())));

		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATANEGDEALLIST,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading master(99%)...")
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATANEGDEALLIST)
												.sendAndReceive()).getBytes())));
		
		engine.getEventDispatcher().pushEvent(
				new Event(TradingEventDispatcher.EVENT_DATAANNOUNCEMENT, true)
						.setField(TradingEventDispatcher.PARAM_CODE,
								eventStatus)
						.setField(TradingEventDispatcher.PARAM_MESSAGE,
								"loading master(100%)...")
						.setField(TradingEventDispatcher.PARAM_DATA,
								Utils.compress(((String) blockingRequest
										.get(BlockRequestSynchronized.REQUEST_DATAANNOUNCEMENT)
										.sendAndReceive()).getBytes())));
	}

	@Override
	public void loadOrder(String eventStatus, String param1, String param2, String param3,
			boolean clear) throws Exception {
		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAORDER, true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading order...")
								.setField(TradingEventDispatcher.PARAM_CLEAR,
										new Boolean(clear))
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATAORDER)
												.sendAndReceiveWithParam(
														param1 + "|" + param2+"|"+param3))
												.getBytes())));
	}

	@Override
	public void loadPortfolio(String eventStatus, String param1, String param2,
			boolean clear) throws Exception {
		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATAPORTFOLIO,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading portfolio...")
								.setField(TradingEventDispatcher.PARAM_CLEAR,
										new Boolean(clear))
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATAPORTFOLIO)
												.sendAndReceiveWithParam(
														param1 + "|" + param2))
												.getBytes())));
	}

	@Override
	public void loadTrade(String eventStatus, String param1) throws Exception {
		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATATRADE, true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading trade...")
								.setField(TradingEventDispatcher.PARAM_CLEAR,
										new Boolean(true))
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATAMATCH)
												.sendAndReceiveWithParam(param1))
												.getBytes())));
	}

	@Override
	public void logon(final String suid, final String spwd) {
		new Thread(new Runnable() {
			public void run() {
				try {
					loadSetting();
					process = true;
					uid = suid;
					pwd = spwd;
					isLogout = false;
					searchingServer();
					// processLogin(suid, spwd);
				} catch (Exception e) {
					//log.info("error");
					e.printStackTrace();
				}
			}
		}).start();
	}

	private boolean process;

	public boolean searchingServer() {
		int i = 0;
		String url;
		boolean bresult = false;
		for (; i <= totalServer && process; i++) {
//			url = (String) hashIP.get(i + "");

			if (Utils.getLiveSims()== 0 ) {
				url = (String) hashIP.get(i + "");
			} else {
				url = "172.17.100.110:7799";
			}
			// rmi = (ThstGtwInterface) Naming.lookup(url);
			String[] urlport = url.split("\\:");
			//log.info("request connect to : " + url + " " + bresult);
			bresult = connect(urlport[0], new Integer(urlport[1]));

			// bresult = rmi != null;
			 log.info("connected trading " + url);
			if (bresult) {
				break;
			} else {
			//log.warn("trying to reconnect (" + i + ")");
			}
		}

		if (!bresult && i >= totalServer) {
			engine.getEventDispatcher()
					.pushEvent(
							new Event(TradingEventDispatcher.EVENT_LOGINFAILED)
									.setField(
											TradingEventDispatcher.PARAM_MESSAGE,
											"Failed, please check your trading connection"));
			return false;
		}

		return bresult;

	}

	protected void processLogin(String suid, String spwd) {
		boolean bresult = false;
		int i = 0;
		int retrying = 3;
		// String url;
		for (; i <= retrying && process; i++) {

			try {
				// url = (String) hashIP.get(i + "");
				//log.info("trying to validate user : " + suid + " " + i);
				// rmi = (ThstGtwInterface) Naming.lookup(url);
				// String[] urlport = url.split("\\:");
				// bresult = connect(urlport[0], new Integer(urlport[1]));
				// bresult = rmi != null;
				// log.info("connected "+bresult);
				if (isConnected()) {
					try {
						// sesid = rmi.login(suid, spwd);
						/*log.info("blocking request is null "
								+ blockingRequest
										.get(BlockRequestSynchronized.REQUEST_LOGIN));*/
						Object obj = blockingRequest.get(
								BlockRequestSynchronized.REQUEST_LOGIN)
								.sendAndReceive("login|" + suid + "|" + spwd);
						sesid = (Long) obj;
						bresult = sesid > 0;
						isValidated = true;
						if (bresult) {
							uid = suid;
							pwd = spwd;
							log.info("request register OK, with session id: "
									+ sesid);
							startServices();
							loadMaster(TradingEventDispatcher.EVENT_LOGINSTATUS);

							UserProfile profile = (UserProfile) engine
									.getDataStore()
									.get(TradingStore.DATA_USERPROFILE)
									.getDataByField(new Object[] { "40" },
											new int[] { UserProfile.C_MENUID });
							// if (profile != null) {
							// log.info("user login with profile : "
							// + profile.getProfileId());
							// if (!profile.getProfileId().toUpperCase()
							// .equals("SUPERUSER")) {
							loadAccount(
									TradingEventDispatcher.EVENT_LOGINSTATUS,
									"%", true);
							loadPortfolio(
									TradingEventDispatcher.EVENT_LOGINSTATUS,
									"%", "%", true);
							 loadSchedule(
									 TradingEventDispatcher.EVENT_LOGINSTATUS,
									 "%", "%", true);
							 loadBrowseOrder(
									 TradingEventDispatcher.EVENT_LOGINSTATUS,
									 "%", true);
							if (!profile.getProfileId().toUpperCase()
									.equals("SUPERUSER")) {
								loadOrder(
										TradingEventDispatcher.EVENT_LOGINSTATUS,
										"%", "%", "%", true);
								loadTrade(
										TradingEventDispatcher.EVENT_LOGINSTATUS,
										"%");

								
							} else {
								//log.info("user is SUPERUSER load data trading will be off");
								loadFilterByClient();
								setFilter(true);
								loadorderbyfilter();
							}
							// }

							getServertime();
//							timerServer = new java.util.Timer();//timer sync yosep
//							reqTimeServer = new RequestTime(this);//timer sync yosep
//							timerServer.schedule(reqTimeServer, 1 * 60 * 1000);//timer sync yosep
//							reqTimeServer.setReq(true);

							engine.getEventDispatcher()
									.pushEvent(
											new Event(
													TradingEventDispatcher.EVENT_NEWSESSIONID)
													.setField(
															TradingEventDispatcher.PARAM_SESSIONID,
															new Long(sesid)));
							engine.getEventDispatcher()
									.pushEvent(
											new Event(
													TradingEventDispatcher.EVENT_LOGINOK));
							bresult = true;

						} else {
							engine.getEventDispatcher()
									.pushEvent(
											new Event(
													TradingEventDispatcher.EVENT_LOGINFAILED)
													.setField(
															TradingEventDispatcher.PARAM_MESSAGE,
															sesid == -2 ? "Failed, please check your trading connection"
																	: "Invalid user/password for trading gateway"));

						}
						break;
					} catch (Exception exp) {
						log.warn("Failed, " + exp.getMessage());
						engine.getEventDispatcher()
								.pushEvent(
										new Event(
												TradingEventDispatcher.EVENT_LOGINSTATUS)
												.setField(
														TradingEventDispatcher.PARAM_MESSAGE,
														"Trading Connection Failed, retrying("
																+ (i + 1)
																+ ")...."));
						try {
							Thread.sleep(500);
						} catch (Exception a) {
						}
						;
						bresult = false;
						//log.error(Utils.logException(exp));
					}
				} else {
					log.warn("Connection Failed, retrying(" + (i + 1) + ")....");
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_LOGINSTATUS)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"Trading Connection Failed, retrying("
															+ (i + 1) + ")...."));
				}
			} catch (Exception ex) {
				log.warn("Connection Failed, retrying(" + i + ")....");
				engine.getEventDispatcher().pushEvent(
						new Event(TradingEventDispatcher.EVENT_LOGINSTATUS)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"Trading Connection Failed, retrying("
												+ (i + 1) + ")...."));
				try {
					Thread.sleep(500);
				} catch (Exception exp) {
				}
				;
				bresult = false;
				//log.error(Utils.logException(ex));
			}
		}

		//log.info("result " + bresult + " " + i + " " + retrying);
		if (!bresult && i > retrying) {
			engine.getEventDispatcher()
					.pushEvent(
							new Event(TradingEventDispatcher.EVENT_LOGINFAILED)
									.setField(
											TradingEventDispatcher.PARAM_MESSAGE,
											"Failed, please check your trading connection"));
		}

	}

	private void loadorderbyfilter() {
		//log.info("load order by filter " + sfilterClient);
		if (sfilterClient != null && !sfilterClient.isEmpty()) {
			String[] sp = sfilterClient.split("#");

			for (String ss : sp) {
				try {
					//log.info("load client " + ss);
					loadOrder(TradingEventDispatcher.EVENT_REFRESHSTATUS, ss,
							"%","%",  false);
					loadTrade(TradingEventDispatcher.EVENT_REFRESHSTATUS, ss);
					addFilterByClient(ss);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void startServices() {

		if (!this.inThread.isAlive())
			inThread.start();

		if (!this.outThread.isAlive())
			outThread.start();
		refreshTimer.start();
		//log.info("service started");
	}

	public boolean connect(String host, int port) {
		boolean isstart = socketConnector.start(host, port);

		return isstart;
	}

	@Override
	public boolean logout() {
		log.info("heartbeat timer stopped, cause : logout called by application");
		refreshTimer.stop();
		process = false;
		sfilterClient = null;
		isReconnect = false;//yosep other device 
		boolean bresult = true;
		try {
			if (socketConnector != null && socketConnector.isConnected()) {
				String istrue = (String) blockingRequest.get(
						BlockRequestSynchronized.REQUEST_LOGOUT)
						.sendAndReceive();
				bresult = istrue.equals("ok");
			}

			// log.info("logout " + bresult);
		} catch (Exception ex) {
			ex.printStackTrace();

		}
		try {
			//log.info("request logout " + bresult);
			isLogout = true;
			if (bresult) {
				sesid = -1;
				outThread.doClear();
				// this.rmi = null;
				/*if ( reqTimeServer != null && timerServer != null ) {
					reqTimeServer.setReq(false);// //timer sync yosep
					timerServer.cancel();//timer sync yosep
				}*/

			}

			if (socketConnector.isConnected())
				socketConnector.closeSocket();

		} catch (Exception ex) {
			ex.printStackTrace();
			bresult = false;
		}
		return bresult;
	}

	@Override
	public void reconnect() {
		sesid = -1;
		//log.info("heartbeat timer stopped, cause : reconnect called by application");
		refreshTimer.stop();
		engine.getEventDispatcher().pushEvent(
				new Event(TradingEventDispatcher.EVENT_DISCONNECT));
	}

	@Override
	public void reconnect(final int count) {
		new Thread(new Runnable() {
			public void run() {
				doReconnect(count);
			}
		}).start();
	}

	private int flip = 1, nextflip = 1, cnt = 0;
	private boolean isFilterOrder, isReconnect = false;

	public void processReconnect() {
		try {
			Object obj = blockingRequest.get(
					BlockRequestSynchronized.REQUEST_LOGIN).sendAndReceive(
					"login|" + uid + "|" + pwd);
			sesid = (Long) obj;

			boolean bresult = sesid > 0;
			isValidated = true;
			if (bresult) {
				startServices();
				engine.getEventDispatcher().pushEvent(
						new Event(TradingEventDispatcher.EVENT_NEWSESSIONID)
								.setField(
										TradingEventDispatcher.PARAM_SESSIONID,
										new Long(sesid)));
				//log.info("ok, we are back to live with new sessionid: " + sesid);
				engine.getEventDispatcher().pushEvent(
						new Event(TradingEventDispatcher.EVENT_RECONNECTOK));
				isReconnect = false;
				
				UserProfile profile = (UserProfile) engine
				.getDataStore()
				.get(TradingStore.DATA_USERPROFILE)
				.getDataByField(new Object[] { "40" },
				new int[] { UserProfile.C_MENUID });

				if (profile.getProfileId().toUpperCase().equals("SUPERUSER")) {
					
				log.info("user is SUPERUSER load data trading will be off");
				setFilter(true);
				loadFilterByClient();

				loadorderbyfilter();

				}
			} else {
				//log.warn("reconnecting failed");
				engine.getEventDispatcher()
						.pushEvent(
								new Event(
										TradingEventDispatcher.EVENT_RECONNECTSTATUS)
										.setField(
												TradingEventDispatcher.PARAM_MESSAGE,
												"reconnecting failed or server not ready"));

				/*
				 * engine.getEventDispatcher().pushEvent( new
				 * Event(TradingEventDispatcher.EVENT_RECONNECTFAILED)
				 * .setField(TradingEventDispatcher.PARAM_MESSAGE,
				 * "reconnecting failed or server not ready"));
				 */

			}
		} catch (Exception e) {
			e.printStackTrace();
			if (e.getMessage().contains("session exception")) {
				processReconnect();
			}
		}
	}

	public void doReconnect(int counter) {

		boolean bresult = false;
		isLogout = false;
		try {
			cnt++;
			engine.getEventDispatcher().pushEvent(
					new Event(TradingEventDispatcher.EVENT_RECONNECTSTATUS)
							.setField(TradingEventDispatcher.PARAM_MESSAGE,
									"trying reconnecting..."));
			if (cnt == nextflip) {
				flip++;
				nextflip = nextflip + 1;
			}
			if (cnt > totalServer * 1) {
				flip = 0;
				nextflip = 0;
				cnt = 0;
			}
			/*log.info("IP Server " + hashIP.toString() + " " + flip + " "
					+ totalServer);*/
			String url;
			if (Utils.getLiveSims()== 0 ) {
				url = (String) hashIP.get(new String(flip + ""));
			} else {
				url = "172.17.100.110:7799";
			}
			// rmi = (ThstGtwInterface) Naming.lookup(url);
			String[] urlport = url.split("\\:");
			// for (; !bresult;) {
			bresult = connect(urlport[0], new Integer(urlport[1]));
			//log.info("doReconnect:" + url + " " + bresult);
			// }
			if (bresult) {
				isReconnect = true;
			} else {
				log.warn("reconnecting failed");
				/*
				 * engine.getEventDispatcher() .pushEvent( new Event(
				 * TradingEventDispatcher.EVENT_RECONNECTSTATUS) .setField(
				 * TradingEventDispatcher.PARAM_MESSAGE,
				 * "reconnecting failed or server not ready"));
				 */
				engine.getEventDispatcher()
						.pushEvent(
								new Event(
										TradingEventDispatcher.EVENT_RECONNECTFAILED)
										.setField(
												TradingEventDispatcher.PARAM_MESSAGE,
												"reconnecting failed or server not ready"));
			}
		} catch (Exception ex) {
			log.warn("reconnecting failed");
			engine.getEventDispatcher().pushEvent(
					new Event(TradingEventDispatcher.EVENT_RECONNECTFAILED)
							.setField(TradingEventDispatcher.PARAM_MESSAGE,
									"reconnecting failed or server not ready"));
			//log.error(Utils.logException(ex));
			ex.printStackTrace();
		}

	}

	@Override
	public void refreshAccount(final String param) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					loadAccount(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							param, false);
					engine.getEventDispatcher()
					.pushEvent(
							new Event(
									TradingEventDispatcher.EVENT_REFRESHACCOUNTID));
				} catch (Exception ex) {
				}
			}

		}).start();
	}

	@Override
	public void refreshCashColl(final String param) {
		new Thread(new Runnable() {
			public void run() {
				try {
					loadCashColl(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							param, false);
				} catch (Exception ex) {
				}
			}
		}).start();
	}

	@Override
	public void refreshData(final String tradingid) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					loadAccount(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							tradingid, false);
					loadSchedule(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							 tradingid, "%", false);
					loadPortfolio(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							tradingid, "%", false);
					loadOrder(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							tradingid, "%","%",  false);
					loadTrade(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							tradingid);
					 loadBrowseOrder(TradingEventDispatcher.EVENT_REFRESHSTATUS
								 , tradingid, false);
					engine.getEventDispatcher().pushEvent(
							new Event(TradingEventDispatcher.EVENT_REFRESHOK));
				} catch (Exception ex) {
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_REFRESHFAILED)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"refresh failed, please try again"));
				}
			}
		}).start();
	}

	@Override
	public void refreshData(final boolean master, final boolean account,
			final boolean pf, final boolean order) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (master)
						loadMaster(TradingEventDispatcher.EVENT_REFRESHSTATUS);
					if (account)
						loadAccount(TradingEventDispatcher.EVENT_REFRESHSTATUS,
								"%", true);
					if (pf)
						loadPortfolio(
								TradingEventDispatcher.EVENT_REFRESHSTATUS,
								"%", "%", true);
					if (order) {
						loadOrder(TradingEventDispatcher.EVENT_REFRESHSTATUS,
								"%", "%","%",  true);
						loadTrade(TradingEventDispatcher.EVENT_REFRESHSTATUS,
								"%");
					}
					engine.getEventDispatcher().pushEvent(
							new Event(TradingEventDispatcher.EVENT_REFRESHOK));
				} catch (Exception ex) {
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_REFRESHFAILED)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"refresh failed, please try again"));
				}
			}
		}).start();
	}

	@Override
	public void refreshOrder(final String param1, final String param2, final String param3) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					System.out.println("-"+param1+" - "+param2+" - "+param3);
					loadOrder(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							param1, param2,param3, false);
					engine.getEventDispatcher().pushEvent(
							new Event(TradingEventDispatcher.EVENT_REFRESHOK));
				} catch (Exception ex) {
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_REFRESHFAILED)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"refresh failed, please try again"));
				}
			}
		}).start();
	}

//*gtc awal
	public void refreshGTC(final String param1, final String param2) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					loadGTC(TradingEventDispatcher.EVENT_REFRESH_GTC,
							param1, param2,true);
				} catch (Exception ex) {
				}
			}
		}).start();
	}
	
	public void loadGTC(final String eventStatus, final String param2) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					engine.getEventDispatcher()
					.pushEvent(
							new Event(TradingEventDispatcher.EVENT_DATAACCOUNTTYPE,
									true)
									.setField(TradingEventDispatcher.PARAM_CODE,
											eventStatus)
									.setField(
											TradingEventDispatcher.PARAM_DATA,
											Utils.compress(((String) blockingRequest
													.get(BlockRequestSynchronized.REQUEST_DATAACCTYPE)
													.sendAndReceive()).getBytes())));
			engine.getEventDispatcher();
					
				} catch (Exception ex) {
				}
			}
		}).start();
	}
//*gtc akhir
	@Override
	public void refreshPortfolio(final String param1, final String param2) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					loadPortfolio(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							param1, param2, false);
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_REFRESHPORTFOLIO));
				} catch (Exception ex) {
				}
			}
		}).start();
	}

	@Override
	public void refreshTrade(final String param1) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					loadTrade(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							param1);
					engine.getEventDispatcher().pushEvent(
							new Event(TradingEventDispatcher.EVENT_REFRESHOK));
				} catch (Exception ex) {
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_REFRESHFAILED)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"refresh failed, please try again"));
				}
			}
		}).start();
	}

	@Override
	public boolean sendEvent(Event event) {
		outThread.addOut(event);
		return true;
	}

	@Override
	public String splitOrder(String param) {
		JSONObject result = new JSONObject();
		result.put("psukses", "0");
		result.put("desc", "not implemented by server");
		result.put("pacc", new JSONArray());
		result.put("porder", new JSONArray());
		result.put("pmatch", new JSONArray());
		//log.info("users split order:" + param);
		try {
			// byte[] hasil = rmi.entrySplitDone(sesid,
			// Utils.compress(param.getBytes()));
			String hsl = (String) blockingRequest.get(
					BlockRequestSynchronized.REQUEST_SPLITDONE)
					.sendAndReceiveWithParam(param);
			System.out.println(hsl);
			result = (JSONObject) ((JSONArray) JSONValue.parse(hsl)).get(0);
			((TradingEventDispatcher) engine.getEventDispatcher())
					.doSplit(result);
		} catch (Exception ex) {
			ex.printStackTrace();

		}
		//log.info("split.order:" + result.toString());
		return result.toString();
	}

	@Override
	public long getSessionid() {
		return sesid;
	}

	public static void main(String[] args) {
		/*
		 * PropertyConfigurator.configure("log.properties"); TradingEngine
		 * engine = new TradingEngine(); engine.start(true);
		 * engine.login("KEVINY", Utils.getMD5("123"));
		 */
	}

	@Override
	public void connected(ClientSocket sock) {
		log.info("connected client " + isReconnect);
		isValidated = false;
		new Thread() {

			@Override
			public void run() {

				if (!isReconnect)
					processLogin(uid, pwd);
				else
					processReconnect();
			}

		}.start();

	}

	@Override
	public void disconnect(ClientSocket sock) {
		log.info("disconnected client " + sock + " " + isLogout + " "
				+ isValidated+" "+isReconnect);
		blockingRequest.clearWaitingStateClient();

		if (sock != null && !isLogout && isValidated) {
			reconnect();
		}
	}

	@Override
	public void receive(ClientSocket sock, byte[] data) {
		if (data != null) {
			byte[] bt = Utils.decompress(data);
			String sdata = new String(bt);
			logFile.info("receive trading "+sdata+" "+data.length+" "+bt.length);
			if (blockingRequest.isBlockingRequest(sdata)) {
				String type = sdata.split("\\|")[0];
				blockingRequest.get(type).addReceive(sdata);
			} else {
				sdata = sdata.replace("|", "\001");
				Vector v = new Vector();
				v.add(sdata);
				inThread.addIn(v);
			}
		}
	}

	@Override
	public boolean atsWithdraw(Object data) throws Exception {
		socketConnector.sendMessage(data);
		return true;
	}

	@Override
	public boolean createAts(Object data) throws Exception {
		socketConnector.sendMessage(data);
		return true;
	}

	@Override
	public boolean notification(Object param1) throws Exception {
//		Object obj = blockingRequest.get(
//				BlockRequestSynchronized.REQUEST_NOTIFICATION)
//				.sendAndReceiveWithParam(param1);
		socketConnector.sendMessage(param1);
		return true;
	}

	@Override
	public void refreshAts(final String param) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					 loadSchedule(TradingEventDispatcher.EVENT_REFRESHSTATUS,
					 param,"%", false);
				} catch (Exception ex) {

				}
			}
		}).start();
	}

	@Override
	public void refreshBrowseAts(final String param) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					 loadBrowseOrder(TradingEventDispatcher.EVENT_REFRESHSTATUS,
					 param, false);
				} catch (Exception ex) {

				}
			}
		}).start();
	}

	@Override
	public void refreshBuySellDetail(final String accid, final String buyorsell) {
		new Thread(new Runnable() {
			public void run() {
				try {
					loadBuySellDetail(
							TradingEventDispatcher.EVENT_REFRESHSTATUS, accid,
							buyorsell, false);
				} catch (Exception ex) {
				}
			}
		}).start();

	}

	protected void loadBuySellDetail(String eventStatus, String accid,
			String buyorsell, boolean b) throws Exception {
		engine.getEventDispatcher()
				.pushEvent(
						new Event(
								TradingEventDispatcher.EVENT_DATABUYSELLDETAIL,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading buy/sell detail...")
								.setField(TradingEventDispatcher.PARAM_CLEAR,
										new Boolean(b))
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_BUYSELLDETAIL)
												.sendAndReceiveWithParam(accid,
														buyorsell)).getBytes())));
	}

	@Override
	public void refreshData(final boolean master, final boolean account,
			final boolean margin, final boolean pf, final boolean order) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (master)
						loadMaster(TradingEventDispatcher.EVENT_REFRESHSTATUS);
					if (account)
						loadAccount(TradingEventDispatcher.EVENT_REFRESHSTATUS,
								"%", true);
					/*
					 * if(margin)//margin
					 * loadMargin(TradingEventDispatcher.EVENT_REFRESHSTATUS,
					 * "%", true);
					 */
					if (pf)
						loadPortfolio(
								TradingEventDispatcher.EVENT_REFRESHSTATUS,
								"%", "%", true);
					if (order) {
						loadOrder(TradingEventDispatcher.EVENT_REFRESHSTATUS,
								"%", "%","%",  true);
						loadTrade(TradingEventDispatcher.EVENT_REFRESHSTATUS,
								"%");
					}
					engine.getEventDispatcher().pushEvent(
							new Event(TradingEventDispatcher.EVENT_REFRESHOK));
				} catch (Exception ex) {
					engine.getEventDispatcher()
							.pushEvent(
									new Event(
											TradingEventDispatcher.EVENT_REFRESHFAILED)
											.setField(
													TradingEventDispatcher.PARAM_MESSAGE,
													"refresh failed, please try again"));
				}
			}
		}).start();
	}

	@Override
	public void refreshDueDate(final String param1) {
		new Thread(new Runnable() {
			public void run() {
				try {
					loadDueDate(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							param1, false);
				} catch (Exception ex) {
					System.out.println("------" + ex);
				}
			}
		}).start();
	}

	public void loadDueDate(String eventStatus, String param1, boolean clear)
			throws Exception {
		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATADUEDATE,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading DueDate ...")
								.setField(TradingEventDispatcher.PARAM_CLEAR,
										new Boolean(clear))
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATADUEDATE)
												.sendAndReceive(
														sesid + "|duedate|"
																+ param1))
												.getBytes())));
	}
	
	@Override
	public void loadSchedule(String eventStatus, String param1, String param2,
			boolean clear) throws Exception {
		engine.getEventDispatcher().pushEvent(
				  new Event(TradingEventDispatcher.EVENT_DATASCHEDULE,true)
				  .setField(TradingEventDispatcher.PARAM_CODE, eventStatus)
				  .setField(TradingEventDispatcher.PARAM_MESSAGE, "loading Schedule....")
				  .setField(TradingEventDispatcher.PARAM_CLEAR, new Boolean(clear))
				  .setField(
						  TradingEventDispatcher.PARAM_DATA, 
						  Utils.compress(((String) blockingRequest
									.get(BlockRequestSynchronized.REQUEST_DATASCHEDULE)
									.sendAndReceiveWithParam(
											param1 + "|" + param2))
									.getBytes())));
	}

	@Override
	public void loadBrowseOrder(String eventStatus, String param1, boolean clear)
			throws Exception {
		engine.getEventDispatcher().pushEvent( new
				  Event(TradingEventDispatcher.EVENT_DATABROWSEORDER,true)
				  .setField(TradingEventDispatcher.PARAM_CODE, eventStatus)
				  .setField(TradingEventDispatcher.PARAM_MESSAGE, "loading browser....")
				  .setField(TradingEventDispatcher.PARAM_CLEAR, new Boolean(clear))
				  .setField(
						  TradingEventDispatcher.PARAM_DATA,  
						  Utils.compress(((String) blockingRequest
							.get(BlockRequestSynchronized.REQUEST_DATABROWSER)
							.sendAndReceiveWithParam(
									sesid+"|browser"))
							.getBytes())));
	}
		
	@Override
	public void refreshNotif(final String param) {
		new Thread(new Runnable() {
			public void run() {
				try {
					loadNotif(TradingEventDispatcher.EVENT_REFRESHSTATUS,
							param, false);
				} catch (Exception ex) {
					System.out.println("+++++" + ex);
				}
			}
		}).start();
	}

	public void loadNotif(String eventStatus, String param1, boolean clear)
			throws Exception {
		// TODO:
		engine.getEventDispatcher()
				.pushEvent(
						new Event(TradingEventDispatcher.EVENT_DATABACKNOTIF,
								true)
								.setField(TradingEventDispatcher.PARAM_CODE,
										eventStatus)
								.setField(TradingEventDispatcher.PARAM_MESSAGE,
										"loading Notif ...")
								.setField(TradingEventDispatcher.PARAM_CLEAR,
										new Boolean(clear))
								.setField(
										TradingEventDispatcher.PARAM_DATA,
										Utils.compress(((String) blockingRequest
												.get(BlockRequestSynchronized.REQUEST_DATABACKNOTIF)
												.sendAndReceiveWithParam(param1))
												.getBytes())));
	}

	public boolean send(String msg) {
		if (socketConnector.isConnected()) {
			socketConnector.sendMessage(Utils.compress(msg.getBytes()));
			return true;
		}

		return false;
	}

	@Override
	public void addFilterByClient(String filter) {
		if (this.isFilterOrder) {
			String msg = getSessionid() + "|filter.client|add|" + filter;
			socketConnector.sendMessage(Utils.compress(msg.getBytes()));
		}
	}

	public void saveFilterByClient(String filter) {
		try {
			blockingRequest.get(
					BlockRequestSynchronized.REQUEST_DATAFILTERCLIENT_SAVE)
					.sendAndReceiveWithParam(filter);
			this.sfilterClient = filter;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String loadFilterByClient() {

		if (sfilterClient == null) {

			try {
				String sf = (String) blockingRequest.get(
						BlockRequestSynchronized.REQUEST_DATAFILTERCLIENT_LOAD)
						.sendAndReceive();

				JSONArray array = (JSONArray) JSONValue.parse(sf);
				JSONObject s1 = (JSONObject) array.get(0);
				if (s1.get("filter") != null) {
					sfilterClient = s1.get("filter").toString();
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return sfilterClient;
	}

	@Override
	public void deleteFilterByClient(String filter) {
		if (this.isFilterOrder) {
			String msg = getSessionid() + "|filter.client|delete|" + filter;
			socketConnector.sendMessage(Utils.compress(msg.getBytes()));
		}
	}

	@Override
	public void setFilter(boolean isfilter) {
		this.isFilterOrder = isfilter;
		//log.info("setFilterOrder:" + isfilter);
		socketConnector.sendMessage(Utils.compress(new String(getSessionid()
				+ "|filter.client|setFilter|" + (isfilter ? "true" : "false")+"|"+uid)
				.getBytes()));
	}

	@Override
	public void addFilterByOrder(String filter) {
		if (this.isFilterOrder) {
			String msg = getSessionid() + "|filter.order|add|" + filter;
			socketConnector.sendMessage(Utils.compress(msg.getBytes()));
		}
	}

	@Override
	public void deleteFilterByOrder(String filter) {
		if (this.isFilterOrder) {
			String msg = getSessionid() + "|filter.order|delete|" + filter;
			socketConnector.sendMessage(Utils.compress(msg.getBytes()));
		}
	}

	
	@Override
	public void refreshOrderMatrix(final String param) {
		new Thread(new Runnable() {
			public void run() {
				try {
					System.out.println(param+" -- ");
					loadOrderMatrix(TradingEventDispatcher.EVENT_REFRESHSTATUS, param, true);
				} catch (Exception ex) {
				}
			}
		}).start();
	}
//*gtc awal
	@Override
	public void gtc(Order gtc) 
	throws Exception {
		// TODO Auto-generated method stub
		engine.getEventDispatcher()
		.pushEvent(
				new Event(TradingEventDispatcher.EVENT_DATAACCOUNT,
						true)
						.setField(TradingEventDispatcher.PARAM_MESSAGE,
								"loading account...")
						.setField(
								TradingEventDispatcher.PARAM_DATA,
								Utils.compress(((String) blockingRequest
										.get(BlockRequestSynchronized.REQUEST_DATAACCOUNT)
										.sendAndReceive() )
										.getBytes())));
	}
	
	@Override
	public boolean createGtc(Object data) throws Exception {
		// TODO Auto-generated method stub
		socketConnector.sendMessage(data);
		return true;
	}
	@Override
	public void loadGTC(String arg0, String arg1, String arg2, boolean arg3)
			throws Exception {
		engine.getEventDispatcher()
		.pushEvent(
				new Event(TradingEventDispatcher.EVENT_DATAGTC,
						true)
						.setField(TradingEventDispatcher.PARAM_CODE,
						"eventStatus")
						.setField(TradingEventDispatcher.PARAM_MESSAGE,
								"loading gtc...")
						.setField(
								TradingEventDispatcher.PARAM_DATA,
								Utils.compress(((String) blockingRequest
										.get(BlockRequestSynchronized.REQUEST_DATAGTC)
										.sendAndReceiveWithParam(arg1,arg2))
										.getBytes())));
		// TODO Auto-generated method stub
		
	}
//*gtc akhir
	@Override
	public void refreshOrderMatrixList(final String param,final String param2) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					loadOrderMatrixList(TradingEventDispatcher.EVENT_REFRESHSTATUS
							, param,param2, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		
	}

	@Override
	public void loadOrderMatrix(String eventStatus,String param1, boolean clear) throws Exception {
			engine.getEventDispatcher().pushEvent(
				  new Event(TradingEventDispatcher.EVENT_ORDERMATRIX,true)
				  .setField(TradingEventDispatcher.PARAM_CODE, eventStatus)
				  .setField(TradingEventDispatcher.PARAM_CLEAR, new Boolean(clear))
				  .setField(
						  TradingEventDispatcher.PARAM_DATA, 
						  Utils.compress(((String) blockingRequest
									.get(BlockRequestSynchronized.REQUEST_DATAORDERMATRIX)
									.sendAndReceiveWithParam(
											param1))
									.getBytes())));
	}
	
	@Override
	public void loadOrderMatrixList(String eventStatus,String param1,String param2, boolean clear) throws Exception {
		engine.getEventDispatcher().pushEvent(
				  new Event(TradingEventDispatcher.EVENT_ORDERMATRIXLIST,true)
				  .setField(TradingEventDispatcher.PARAM_CODE, eventStatus)
				  .setField(TradingEventDispatcher.PARAM_CLEAR, new Boolean(clear))
				  .setField(
						  TradingEventDispatcher.PARAM_DATA, 
						  Utils.compress(((String) blockingRequest
									.get(BlockRequestSynchronized.REQUEST_DATAORDERMATRIXLIST)
									.sendAndReceiveWithParam(
											param1+"|"+param2))
									.getBytes())));
	}

	@Override
	public boolean createOrderMatrix(Object data) throws Exception {
		socketConnector.sendMessage(data);
		return true;
	}

	@Override
	public boolean deleteOrderMatrix(Object data)
			throws Exception {
		socketConnector.sendMessage(data);
		return true;
	}

	@Override
	public boolean deleteOrderMatrixDetil(Object arg0) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}
	public void deleteFilterByStock(String filter) {
		if (this.isFilterOrder) {
			String msg = getSessionid() + "|filter.stock|delete|" + filter;
			socketConnector.sendMessage(Utils.compress(msg.getBytes()));
		}
	}
	public void addFilterByStock(String filter) {
		if (this.isFilterOrder) {
			String msg = getSessionid() + "|filter.stock|add|" + filter;
		//	log.info("addFilterByStock:" + msg);
			socketConnector.sendMessage(Utils.compress(msg.getBytes()));
		}
	}

	//timer sync yosep
	class RequestTime extends TimerTask{
		TradingSocketConnection socketConnection ;
		public RequestTime(TradingSocketConnection socketConnection){
			this.socketConnection = socketConnection;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				socketConnection.getServertime();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	

	class ReqTime extends Thread{
		TradingSocketConnection socketConnection;
		boolean req = false;
		ReqTime(TradingSocketConnection connectionSocket){
			socketConnection = connectionSocket;
		}
		
		public void setReq(boolean req){
			this.req = req;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				while (true){
					if (req) {
						socketConnection.getServertime();System.out.println("reqtime trading thread");
						Thread.sleep(1* 60 * 1000);				
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
