package PSI19;

import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ProphetAgent extends Agent {

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
	
		float epsilon = 0.5f;

		int numActions;

		int[] opponentActions;
		int[][] punctuationMatrix;

		private Random random = new Random();

		private int rounds = 0;
		
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
							initObservations();
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
								initObservations();
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
							rounds = 0;
						}
					} else {
						System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
					}
					break;

				case s2Round:

					if(msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().startsWith("Position")) {
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.addReceiver(mainAgent);
						int action = getAction();
						msg.setContent("Position#" + action);
						send(msg);
						state = State.s3AwaitingResult;
					} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Changed#")) {
						float p = validatePositionMessage(msg.getContent());
						reset(p);
					} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("EndGame")){
						reset(0);
						state = State.s1AwaitingGame;
					}
					break;

				case s3AwaitingResult:

					if(msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
						int[] valores = validateResultsMessage(msg.getContent());
						feedback(valores);
						state = State.s2Round;
						rounds++; //TODO
					} else {
						//System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
					}
					break;
				}
			}
		}

		private void initObservations() {
			//TODO

			opponentActions = new int[S];
			punctuationMatrix = new int[S][S];

			
			for(int i = 0; i < S; i++) {
				opponentActions[i] = 0;
			}
			
			for(int i = 0; i < S; i++) {
				for(int j = 0; j < S; j++) {
					punctuationMatrix[i][j] = -1;
				}
			}
			
		}

		private void reset(float p) {
			//volvemos a empezar de cero
			//TODO

			initObservations();
		}

		private float validatePositionMessage(String content) {
			
			String[] contentSplit = content.split("#");
			if(contentSplit.length != 2) return -1;
			return Float.parseFloat(contentSplit[1]);
		}

		private int[] validateResultsMessage(String content) {
			// Results#pos1,pos2#r1,r2
			
			String[] contentSplit = content.split("#");

			String[] result1Split = contentSplit[1].split(",");
			String[] result2Split = contentSplit[2].split(",");

			int[] valores = new int[4];
			
			if(myId < opponentId) {
				valores[0] = Integer.parseInt(result1Split[1]);//opponent pos
				valores[1] = Integer.parseInt(result1Split[0]);
				valores[2] = Integer.parseInt(result2Split[1]);//opponent punct
				valores[3] = Integer.parseInt(result2Split[0]);
			} else {
				valores[0] = Integer.parseInt(result1Split[0]);
				valores[1] = Integer.parseInt(result1Split[1]);
				valores[2] = Integer.parseInt(result2Split[0]);
				valores[3] = Integer.parseInt(result2Split[1]);
			}
			
			
			return valores;
		}

		private void feedback(int[] valores) {

			//TODO
			opponentActions[valores[0]] += 1;
			
			punctuationMatrix[valores[0]][valores[1]] = valores[3];
			//opp pos, my pos, my value
			
			return;
		}

		private int predictAction() {
			
			//TODO
			int prediction = 0;
			
			double[] actionPr = new double[S];
			
			for(int i = 0; i < S; i ++) {
				
				actionPr[i] = (double) opponentActions[i] / (double) rounds;
			}
			
			double cúmulo = 0;
			double umbralAleatorio = Math.random();
			for(int i = 0; i< numActions; i++) {
				cúmulo += cúmulo + actionPr[i];
				if(umbralAleatorio < cúmulo) {
					prediction = i;
					break;
				}
			}
			
			System.out.println("Prediction: " + prediction );
			return prediction;
		}
		
		private int getAction() {
			
			//TODO

			int prediction = predictAction();
			int action = 0;
			
			//si no conozco nada
			int[] row = punctuationMatrix[prediction];

			int unknown = 0;
			int max = -1;
			int maxIndex = 0;
			
			for(int i = 0; i < S; i ++) {
				
				int value = row[i];
				
				if(value == -1) {
					unknown++;
				}
				
				if(value > max) {
					max = value;
					maxIndex = i;
				}
			}
			
			System.out.println("MaxIndex " + maxIndex);
			
			if(unknown == S) {
				
				action = random.nextInt(S);
				
			} else if(unknown > 0) {
				
				if(random .nextFloat() < epsilon) {
					
					action = random.nextInt(S);
				} else {
					
					action = maxIndex;
				}
			} else {
				
				action = maxIndex ;
			}
			
			System.out.println("Action " + action);

			return action;
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
