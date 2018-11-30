package PSI19;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class LAAgent extends Agent {

	private State state;
	private AID mainAgent;
	private int myId, opponentId;
	private int N, S, R, I, P;
	private ACLMessage msg;

	protected void setup() {

		state = State.s0NoConfig;

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		sd.setName("Game");
		dfd.addServices(sd);

		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new Play());
	}

	protected void takeDown() {

		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	private enum State{
		s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
	}

	private class Play extends CyclicBehaviour {

		private double dLearnRate = 0.1;		

		int iNumActions;
		int iLastAction;					// The last action that has been played by this player
		StateAction stateAction;			// Contains the present state we are and the actions that are available

		private int ultimaPuntuacion = 4;

		public void action() {

			msg = blockingReceive();

			if(msg != null) {

				switch(state) {

				case s0NoConfig:

					if(msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
						boolean parametersUpdated = false;
						try {
							parametersUpdated = validateSetupMessage(msg);							
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
						if(parametersUpdated) {
							iNumActions = S;
							stateAction = new StateAction("s0", iNumActions, true);
							state = State.s1AwaitingGame;
						}

					} else {
						System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
					}
					break;

				case s1AwaitingGame:

					if(msg.getPerformative() == ACLMessage.INFORM) {
						if(msg.getContent().startsWith("Id#")) {
							try {
								validateSetupMessage(msg);
								iNumActions = S;
								stateAction = new StateAction("s0", iNumActions, true);
								System.out.println("@@@@@@@@@@@@   " + iNumActions);
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
						} else if (msg.getContent().startsWith("NewGame#")) {
							boolean gameStarted = false;
							try {
								gameStarted = validateNewGame(msg.getContent());
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
							if (gameStarted) state = State.s2Round;
						}
					} else {
						System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
					}
					break;

				case s2Round:

					if(msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().startsWith("Position")) {
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.addReceiver(mainAgent);
						int action = getLAAction();
						msg.setContent("Position#" + action);
						send(msg);
						state = State.s3AwaitingResult;
					} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Changed#")) {
						int p = validatePositionMessage(msg.getContent());
						resetLA(p);
					} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("EndGame")){
						resetLA(0);
						state = State.s1AwaitingGame;
					}
					break;

				case s3AwaitingResult:

					if(msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
						int puntuacion = validateResultsMessage(msg.getContent())[0];
						feedbackLAAction(puntuacion);
						state = State.s2Round;
					} else {
						//System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
					}
					break;
				}
			}
		}

		private void resetLA(int p) {
			//volvemos a empezar de cero
			
			ultimaPuntuacion = 4;
			stateAction = new StateAction("s0", iNumActions, true);
		}

		private int validatePositionMessage(String content) {
			
			String[] contentSplit = content.split("#");
			if(contentSplit.length != 2) return -1;
			return Integer.parseInt(contentSplit[1]);
		}

		private int[] validateResultsMessage(String content) {
			// Results#id1,id2#r1,r2
			
			String[] contentSplit = content.split("#");
			if(contentSplit.length != 3) return null;
			String[] result1Split = contentSplit[1].split(",");
			String[] result2Split = contentSplit[2].split(",");
			if(result1Split.length != 2 || result2Split.length != 2) return null;
			
			int[] puntuacion = new int[2];
			if(myId == Integer.parseInt(result1Split[0])) {
				puntuacion[0] = Integer.parseInt(result2Split[0]);
				puntuacion[1] = Integer.parseInt(result2Split[1]);
			} else if(myId == Integer.parseInt(result1Split[1])) {
				puntuacion[0] = Integer.parseInt(result2Split[1]);
				puntuacion[1] = Integer.parseInt(result2Split[0]);				
			}
			return puntuacion;
		}

		private void feedbackLAAction(int puntuacion) {

			//ajustamos probabilidades
			if (puntuacion - ultimaPuntuacion > 0)	
				for (int i=0; i<iNumActions; i++)
					if (i == iLastAction)
						stateAction.dValAction[i] += dLearnRate * (1.0 - stateAction.dValAction[i]);	// Reinforce the last action
					else
						stateAction.dValAction[i] *= (1.0 - dLearnRate);		// The rest are weakened


			//actualizamos valores
			ultimaPuntuacion = puntuacion;

			for(int i = 0; i < iNumActions; i++) {
				System.out.println("distrib " + i + ": " + stateAction.dValAction[i]);
			}
		}

		private int getLAAction() {

			//consideramos que el estado es siempre el mismo ya que la matriz es invariable

			//obtenemos la acción

			double dValAcc = 0;
			double dValRandom = Math.random();
			for(int i = 0; i< iNumActions; i++) {
				dValAcc += stateAction.dValAction[i];
				if(dValRandom < dValAcc ) {
					iLastAction = i;
					break;
				}
			}

			return iLastAction;
		}

		public class StateAction
		{
			String sState;
			double[] dValAction;

			StateAction (String sAuxState, int iNActions) {
				sState = sAuxState;
				dValAction = new double[iNActions];
			}

			StateAction (String sAuxState, int iNActions, boolean bLA) {
				this (sAuxState, iNActions);
				if (bLA) for (int i=0; i<iNActions; i++)	// This constructor is used for LA and sets up initial probabilities
					dValAction[i] = 1.0 / iNActions;
			}

			public String sGetState() {
				return sState;
			}

			public double dGetQAction (int i) {
				return dValAction[i];
			}
		}

	}

	/**
	 * Validates and extracts the parameters from the setup message
	 *
	 * @param msg ACLMessage to process
	 * @return true on success, false on failure
	 */
	private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
		int tN, tS, tR, tI, tP, tMyId;
		String msgContent = msg.getContent();

		String[] contentSplit = msgContent.split("#");
		if (contentSplit.length != 3) return false;
		if (!contentSplit[0].equals("Id")) return false;
		tMyId = Integer.parseInt(contentSplit[1]);

		String[] parametersSplit = contentSplit[2].split(",");
		if (parametersSplit.length != 5) return false;
		tN = Integer.parseInt(parametersSplit[0]);
		tS = Integer.parseInt(parametersSplit[1]);
		tR = Integer.parseInt(parametersSplit[2]);
		tI = Integer.parseInt(parametersSplit[3]);
		tP = Integer.parseInt(parametersSplit[4]);

		//At this point everything should be fine, updating class variables
		mainAgent = msg.getSender();
		N = tN;
		S = tS;
		R = tR;
		I = tI;
		P = tP;
		myId = tMyId;
		return true;
	}

	/**
	 * Processes the contents of the New Game message
	 * @param msgContent Content of the message
	 * @return true if the message is valid
	 */
	public boolean validateNewGame(String msgContent) {
		int msgId0, msgId1;
		String[] contentSplit = msgContent.split("#");
		if (contentSplit.length != 2) return false;
		if (!contentSplit[0].equals("NewGame")) return false;
		String[] idSplit = contentSplit[1].split(",");
		if (idSplit.length != 2) return false;
		msgId0 = Integer.parseInt(idSplit[0]);
		msgId1 = Integer.parseInt(idSplit[1]);
		if (myId == msgId0) {
			opponentId = msgId1;
			return true;
		} else if (myId == msgId1) {
			opponentId = msgId0;
			return true;
		}
		return false;
	}
}
