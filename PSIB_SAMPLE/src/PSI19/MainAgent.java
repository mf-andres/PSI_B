package PSI19;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class MainAgent extends Agent {

	private GUI gui;
	private AID[] playerAgents;
	private GameParametersStruct parameters = new GameParametersStruct();
	private Semaphore playGameMutex = new Semaphore(0);
	
	@Override
	protected void setup() {

		gui = new GUI(this);
		System.setOut(new PrintStream(gui.getLoggingOutputStream()));

		updatePlayers();
		gui.setParametersLabel(parameters.toString());
		gui.logLine("Agent " + getAID().getName() + " is ready.");
	}

	public int updatePlayers() {

		gui.logLine("Updating player list");

		//prepara la búsqueda
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		template.addServices(sd);

		//busca 
		try {

			DFAgentDescription[] result = DFService.search(this, template);
			if (result.length > 0) {

				gui.logLine("Found " + result.length + " players");
			}

			//inicializa playerAgents
			playerAgents = new AID[result.length];
			for (int i = 0; i < result.length; ++i) {

				playerAgents[i] = result[i].getName();
			}

		} catch (FIPAException fe) {

			gui.logLine(fe.getMessage());
		}

		//Imprime los agentes encontrados
		String[] playerNames = new String[playerAgents.length];
		for (int i = 0; i < playerAgents.length; i++) {

			playerNames[i] = playerAgents[i].getName();
		}
		gui.setPlayersUI(playerNames);

		//Desbloquea el botón de new game
		if(playerAgents.length >= parameters.N) {
			
			gui.enableNewButton();
		}
		
		return 0;
	}

	//	public void updatePlayersDinamically() {
	//
	//		gui.logLine("Updating player list dinamicallly");
	//
	//		//template
	//		DFAgentDescription dfd = new DFAgentDescription();
	//		ServiceDescription sd = new ServiceDescription();
	//		sd.setType("Player");
	//		dfd.addServices(sd);
	//		
	//		//subscription
	//		SearchConstraints sc = new SearchConstraints();
	//		send(DFService.createSubscriptionMessage(this, getDefaultDF(), 
	//				dfd, sc));
	//		
	//		//registering loop
	//		int registrations = 0;
	//		ArrayList<AID> registeredAgents = new ArrayList<AID>();
	//		while(registrations < parameters.N) {
	//			
	//			try {
	//			
	//			ACLMessage notification = blockingReceive();
	//			DFAgentDescription[] agentDescription = DFService.decodeNotification(notification.getContent());
	//			AID aid = agentDescription[0].getName();
	//			registeredAgents.add(aid);
	//			gui.logLine("Agent recieved: " + aid);
	//			}  catch (FIPAException fe) {
	//				gui.logLine(fe.getMessage());
	//			}
	//			
	//			gui.logLine("All agents registered");
	//			playerAgents = (AID[]) registeredAgents.toArray();
	//		}
	//	}

	public void setParameters(String paramStr) {

		//procesado
		String[] parts = paramStr.trim().split(",");
		
		if(parts.length != 5) {
			
			System.out.println("setParameters: paramStr split went wrong");
			System.out.println("paramStr = " + paramStr);
			
			gui.log("Incorrect parameters, nothing will be done");
			
			return;
		}
		
		int n = Integer.parseInt(parts[0]);
		int s = Integer.parseInt(parts[1]);
		int r = Integer.parseInt(parts[2]);
		int i = Integer.parseInt(parts[3]);
		int p = Integer.parseInt(parts[4]);
		
		parameters = new GameParametersStruct(n, s, r, i, p);		
		gui.setParametersLabel(parameters.toString());
		
		if(playerAgents.length < parameters.N) {
			
			gui.leftPanelNewButton.setEnabled(false);
		}
	}

	//llamado por la gui
	public int newGame() {

		addBehaviour(new GameManager(parameters));
		return 0;
	}
	
	public void releasePlayGameMutex() {
		
		if(playGameMutex.availablePermits() > 0) {
			
			//playGame has already been allowed
			System.out.println("permits : " + playGameMutex.availablePermits() );
			return;
		}
		
		playGameMutex.release();
	}

	/**
	 * In this behavior this agent manages the course of a match during all the
	 * rounds.
	 */
	private class GameManager extends SimpleBehaviour {

		GameParametersStruct parameters;

		public GameManager(GameParametersStruct parameters) {

			this.parameters = parameters;
		}

		@Override
		public void action() {

			//Assign the IDs
			ArrayList<PlayerInformation> players = new ArrayList<>();
			int lastId = 0;
			for (AID a : playerAgents) {

				players.add(new PlayerInformation(a, lastId++));
			}

			//Initialize (inform ID)
			for (PlayerInformation player : players) {

				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("Id#" + player.id + "#" + parameters.N + "," + parameters.S + "," + parameters.R + "," + parameters.I + "," + parameters.P);
				msg.addReceiver(player.aid);

				send(msg);
			}

			//score matrix
			ScoreMatrix scoreMatrix = new ScoreMatrix(parameters.N);

			//Organize the matches
			int scores[];
			for (int i = 0; i < parameters.N; i++) {

				for (int j = i + 1; j < parameters.N; j++) { 
					
					//games label
					gui.setGamesLabel(Integer.toString(i), Integer.toString(j));
					
					//matrix
					GameMatrix matrix = new GameMatrix(parameters.S);
					gui.setPayoffTable(matrix.getTranslated(), matrix.getColumnNames());
					
					gui.log("press play game to proccess");
					
					//mutex
					//el juego no empieza hasta que el administrador no lo quiera
					try {
						
						playGameMutex.acquire();
					
					} catch (InterruptedException e) {
						
						e.printStackTrace();
					}

					scores = playGame(players.get(i), players.get(j), matrix);
					
					scoreMatrix.updateScore(i, scores[0]);
					scoreMatrix.updateScore(j, scores[1]);

					updateScores(scoreMatrix);				
				}
			}

			gui.log("Main league has ended");
			gui.setGamesLabel();
		}

		private int[] playGame(PlayerInformation player1, PlayerInformation player2,GameMatrix matrix) {

			//inform players of a new game
			//note: player1.id is always lower
			gui.logLine("Main Game between: " + player1.id  + " and " + player2.id);

			gui.logLine("Main Inform NewGame");
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(player1.aid);
			msg.addReceiver(player2.aid);
			msg.setContent("NewGame#" + player1.id + "," + player2.id);
			send(msg);

			int pos1, pos2;
			int rewardP1, rewardP2;
			int p1Score = 0, p2Score = 0;
			
			for(int round = 0; round < parameters.R; round++) {

				//request position
				gui.logLine("Main Request Position");
				
				msg = new ACLMessage(ACLMessage.REQUEST);
				msg.setContent("Position");
				msg.addReceiver(player1.aid);
				send(msg);

				gui.logLine("Main Waiting for player1");
				ACLMessage move1 = blockingReceive();
				gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
				pos1 = Integer.parseInt(move1.getContent().split("#")[1]);

				msg = new ACLMessage(ACLMessage.REQUEST);
				msg.setContent("Position");
				msg.addReceiver(player2.aid);
				send(msg);

				gui.logLine("Main Waiting for player2");
				ACLMessage move2 = blockingReceive();
				gui.logLine("Main Received " + move2.getContent() + " from " + move2.getSender().getName());
				pos2 = Integer.parseInt(move2.getContent().split("#")[1]);

				//get rewards
				rewardP1 = matrix.getRewardP1(pos1, pos2);
				rewardP2 = matrix.getRewardP2(pos1, pos2);

				p1Score += rewardP1;
				p2Score += rewardP2;

				gui.logLine("Main Inform Results");
				msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(player1.aid);
				msg.addReceiver(player2.aid);
				msg.setContent("Results#" + pos1 + "," + pos2 + "#" + rewardP1 + "," + rewardP2);
				send(msg);				

				// TODO queda probar esto de aquí
				//si I es 0 pues la matriz no se altera nunca
				if(round == parameters.I && parameters.I != 0) {
					
					//alter matrix
					float p =  matrix.alter(parameters.P);
					gui.setPayoffTable(matrix.getTranslated(), matrix.getColumnNames());
					
					//inform the players
					gui.logLine("Main Inform Changed");
					msg = new ACLMessage(ACLMessage.INFORM);
					msg.addReceiver(player1.aid);
					msg.addReceiver(player2.aid);
					msg.setContent("Changed#" + p);
					send(msg);

					//mutex
					//el juego no empieza hasta que el administrador no lo quiera
					gui.log("press play game to proccess");
					try {
						
						playGameMutex.acquire();
					
					} catch (InterruptedException e) {
						
						e.printStackTrace();
					}
				}
			}

			gui.logLine("Main Inform EndGame");
			msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(player1.aid);
			msg.addReceiver(player2.aid);
			msg.setContent("EndGame");
			send(msg);

			return new int[]{p1Score, p2Score};
		}

		@Override
		public boolean done() {
			return true;
		}

		private void updateScores(ScoreMatrix scoreMatrix) {

			//Imprime los agentes encontrados
			String[] playerNames = new String[playerAgents.length];
			
			for (int i = 0; i < playerAgents.length; i++) {

				//para contemplar jugadores captados pero que no participen
				String score = "";
				if(i < parameters.N) {
					
					score = " | " + scoreMatrix.getScore(i);	
				}
				
				playerNames[i] = playerAgents[i].getName() + score;
			}
			
			gui.setPlayersUI(playerNames);
		}
	}

	public class PlayerInformation {

		AID aid;
		int id;

		public PlayerInformation(AID a, int i) {
			aid = a;
			id = i;
		}

		@Override
		public boolean equals(Object o) {
			return aid.equals(o);
		}
	}

	public class GameParametersStruct {

		int N;
		int S;
		int R;
		int I;
		int P;

		public GameParametersStruct() {
			N = 2;
			S = 4;
			R = 50;
			I = 0;
			P = 10;
		}

		public GameParametersStruct(int n, int s, int r, int i, int p) {

			super();
			N = n;
			S = s;
			R = r;
			I = i;
			P = p;
		}
		
		public String toString() {
			
			String str;
			
			str = N + ", " + S + ", " + R + ", " + I + ", " + P;
			
			return str;
		}

	}
}
