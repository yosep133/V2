package eqtrade.trading.engine.socket;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eqtrade.trading.core.ITradingEngine;
import eqtrade.trading.engine.socket.block.BaseBlock;

public class BlockRequestSynchronized {
	public final static String REQUEST_TIME = new String("servertime");
	public final static String REQUEST_LOGIN = new String("login");
	public final static String REQUEST_LOGOUT = new String("logout");
	public final static String REQUEST_CHANGEPIN = new String("changepin");
	public final static String REQUEST_CHANGEPASSWORD = new String("chgpwd");
	public final static String REQUEST_CHECKPIN = new String("checkpin");
	public final static String REQUEST_CHECKPIN_NEGO = new String("checkpinnego");
	public final static String REQUEST_SPLITDONE = new String("entrysplitdone");
	public final static String REQUEST_DATE = new String("date");
	public final static String REQUEST_DATAACCTYPE = new String("acctype");
	public final static String REQUEST_DATAINVTYPE = new String("invtype");
	public final static String REQUEST_DATAUSERTYPE = new String("usertype");
	public final static String REQUEST_DATACUSTTYPE = new String("custtype");
	public final static String REQUEST_DATABOARD = new String("board");
	public final static String REQUEST_DATABROKER = new String("broker");
	public final static String REQUEST_DATAEXCHANGE = new String("exchange");
	public final static String REQUEST_DATASTATUSACC = new String("statusacc");
	public final static String REQUEST_DATASTATUSORDER = new String(
			"statusorder");
	public final static String REQUEST_DATAACCTYPESEC = new String("acctypesec");
	public final static String REQUEST_DATASECURITITES = new String(
			"securities");
	public final static String REQUEST_DATAUSERPROFILE = new String(
			"userprofile");
	public final static String REQUEST_DATANEGDEALLIST = new String("negdeal");
	public final static String REQUEST_DATAACCOUNT = new String("account");
	public final static String REQUEST_DATACASHCOLL = new String("cashcoll");
	public final static String REQUEST_DATAORDER = new String("order");
	public final static String REQUEST_DATAPORTFOLIO = new String("portfolio");
	public final static String REQUEST_DATAMATCH = new String("matchorder");
	public final static String REQUEST_DATABACKNOTIF = new String("backnotif");
	public final static String REQUEST_DATADUEDATE = new String("duedate");
	public final static String REQUEST_DATAFILTERCLIENT_LOAD = new String("filter.client.load");
	public final static String REQUEST_DATAFILTERCLIENT_SAVE = new String("filter.client.save");

	public final static String REQUEST_DATASCHEDULE = new String("getAts") ;
	public final static String REQUEST_DATABROWSER = new String("getAtsBrowse");
	public final static String REQUEST_BUYSELLDETAIL = new String(
			"buyselldetail");
	public final static String REQUEST_NOTIFICATION = new String("notification");

	public final static String REQUEST_DATAANNOUNCEMENT = new String("announcement");
	public final static String REQUEST_DATAGTC = new String("GTC");
	
	public final static String REQUEST_DATAORDERMATRIX = new String("ordermatrix");
	public final static String REQUEST_DATAORDERMATRIXLIST = new String("ordermatrixlist");
	
	private HashMap<String, BaseBlock> map = new HashMap<String, BaseBlock>();
	private ITradingEngine engine;
	private Logger log = LoggerFactory.getLogger(getClass());

	public BlockRequestSynchronized(ITradingEngine engine) {
		this.engine = engine;
		init();
	}

	private void init() {
		map.put(REQUEST_TIME, new BaseBlock(REQUEST_TIME, engine) {

			@Override
			protected Object parsing(Object dt) {
				String st = "";
				try {
					st = (String) super.parsing(dt);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (st.isEmpty())
					return new Date().getTime();

				return new Long(st);
			}

		});
		map.put(REQUEST_LOGIN,
				new eqtrade.trading.engine.socket.block.LoginRequest(
						REQUEST_LOGIN, engine));
		map.put(REQUEST_LOGOUT, new BaseBlock(REQUEST_LOGOUT, engine));
		map.put(REQUEST_SPLITDONE, new BaseBlock(REQUEST_SPLITDONE, engine));
		map.put(REQUEST_CHANGEPIN, new BaseBlock(REQUEST_CHANGEPIN, engine));
		map.put(REQUEST_CHANGEPASSWORD, new BaseBlock(REQUEST_CHANGEPASSWORD,
				engine));
		map.put(REQUEST_CHECKPIN, new BaseBlock(REQUEST_CHECKPIN, engine));
		map.put(REQUEST_CHECKPIN_NEGO, new BaseBlock(REQUEST_CHECKPIN_NEGO, engine));
		map.put(REQUEST_DATE, new BaseBlock(REQUEST_DATE, engine));
		map.put(REQUEST_DATAACCTYPE, new BaseBlock(REQUEST_DATAACCTYPE, engine));
		map.put(REQUEST_DATAINVTYPE, new BaseBlock(REQUEST_DATAINVTYPE, engine));
		map.put(REQUEST_DATAUSERTYPE, new BaseBlock(REQUEST_DATAUSERTYPE,
				engine));
		map.put(REQUEST_DATACUSTTYPE, new BaseBlock(REQUEST_DATACUSTTYPE,
				engine));
		map.put(REQUEST_DATAEXCHANGE, new BaseBlock(REQUEST_DATAEXCHANGE,
				engine));
		map.put(REQUEST_DATASTATUSACC, new BaseBlock(REQUEST_DATASTATUSACC,
				engine));
		map.put(REQUEST_DATASTATUSORDER, new BaseBlock(REQUEST_DATASTATUSORDER,
				engine));
		map.put(REQUEST_DATAACCTYPESEC, new BaseBlock(REQUEST_DATAACCTYPESEC,
				engine));
		map.put(REQUEST_DATASECURITITES, new BaseBlock(REQUEST_DATASECURITITES,
				engine));
		map.put(REQUEST_DATAUSERPROFILE, new BaseBlock(REQUEST_DATAUSERPROFILE,
				engine));
		map.put(REQUEST_DATANEGDEALLIST, new BaseBlock(REQUEST_DATANEGDEALLIST,
				engine));
		map.put(REQUEST_DATABOARD, new BaseBlock(REQUEST_DATABOARD, engine));
		map.put(REQUEST_DATABROKER, new BaseBlock(REQUEST_DATABROKER, engine));
		map.put(REQUEST_DATAACCOUNT, new BaseBlock(REQUEST_DATAACCOUNT, engine));
		map.put(REQUEST_DATACASHCOLL, new BaseBlock(REQUEST_DATACASHCOLL,
				engine));
		map.put(REQUEST_DATAORDER, new BaseBlock(REQUEST_DATAORDER, engine));
		map.put(REQUEST_DATAPORTFOLIO, new BaseBlock(REQUEST_DATAPORTFOLIO,
				engine));
		map.put(REQUEST_DATAMATCH, new BaseBlock(REQUEST_DATAMATCH, engine));

		map.put(REQUEST_DATABACKNOTIF, new BaseBlock(REQUEST_DATABACKNOTIF,
				engine));

		map.put(REQUEST_DATADUEDATE, new BaseBlock(REQUEST_DATADUEDATE, engine));

		map.put(REQUEST_BUYSELLDETAIL, new BaseBlock(REQUEST_BUYSELLDETAIL,
				engine));

		map.put(REQUEST_NOTIFICATION, new BaseBlock(REQUEST_NOTIFICATION,
				engine));
		
		map.put(REQUEST_DATAFILTERCLIENT_LOAD, new BaseBlock(REQUEST_DATAFILTERCLIENT_LOAD,
				engine));
		
		map.put(REQUEST_DATAFILTERCLIENT_SAVE, new BaseBlock(REQUEST_DATAFILTERCLIENT_SAVE,
				engine));

		map.put(REQUEST_DATASCHEDULE, new BaseBlock(REQUEST_DATASCHEDULE,
				engine));
		
		map.put(REQUEST_DATABROWSER, new BaseBlock(REQUEST_DATABROWSER, engine));
		 map.put(REQUEST_DATAANNOUNCEMENT, new BaseBlock(REQUEST_DATAANNOUNCEMENT, engine));
		 map.put(REQUEST_DATAORDERMATRIX, new BaseBlock(REQUEST_DATAORDERMATRIX, engine));
		 map.put(REQUEST_DATAORDERMATRIXLIST, new BaseBlock(REQUEST_DATAORDERMATRIXLIST, engine));

		 
		 map.put(REQUEST_DATAGTC, new BaseBlock(REQUEST_DATAGTC, engine));
	}

	public BaseBlock get(String type) {
		return map.get(type);
	}

	public void receive(String msg) {
		String header = msg.split("\\|")[0];
		if (map.containsKey(header)) {
			msg = msg.substring(msg.indexOf("|") + 1, msg.length());
			map.get(header).addReceive(msg);
		} else {
			//log.info("cannot handle receive " + msg);

		}
	}

	public boolean isBlockingRequest(String msg) {
		try {
			String header = msg.split("\\|")[0];
			if (map.containsKey(header)) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void clearWaitingStateClient() {
		Iterator<String> iters = map.keySet().iterator();
		while (iters.hasNext()) {
			String next = iters.next();
			BaseBlock bb = map.get(next);
			bb.clear();
		}
	}

}
