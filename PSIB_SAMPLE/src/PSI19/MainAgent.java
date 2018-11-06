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

public class MainAgent extends Agent {

	private GUI gui;
	private AID[] playerAgents;
	private GameParametersStruct parameters = new GameParametersStruct();

	@Override
	protected void setup() {

		gui = new GUI(this);
		System.setOut(new PrintStream(gui.getLoggingOutputStream()));

		updatePlayers();
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

	//llamado por la gui
	public int newGame() {
		
		addBehaviour(new GameManager(parameters));
		return 0;
	}
	
	public void setParameters(int n, int s, int r, int i, int p) {
		
		parameters = new GameParametersStruct(n, s, r, i, p);
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
			//Organize the matches
			for (int i = 0; i < players.size(); i++) {
				for (int j = i + 1; j < players.size(); j++) { //too lazy to think, let's see if it works or it breaks
					playGame(players.get(i), players.get(j));
				}
			}
		}

		private void playGame(PlayerInformation player1, PlayerInformation player2) {
			
			gui.logLine("Creating matrix");
			GameMatrix matrix = new GameMatrix(parameters.S);
			
			gui.SetPayoffTable(matrix.getTraducted(), matrix.getColumnNames());
			
			//Decide who is player 1 and who is player 2
			if(player2.id < player1.id) {
				
				PlayerInformation temp = player1;
				player1 = player2;
				player2 = temp;
			}
			
			//inform players of a new game
			gui.logLine("Informing of a new game");
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(player1.aid);
            msg.addReceiver(player2.aid);
            msg.setContent("NewGame#" + player1.id + "," + player2.id);
            send(msg);

			//request position
			int pos1, pos2;
			int rewardP1, rewardP2;
			int p1Score = 0, p2Score = 0;

			msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setContent("Position");
			msg.addReceiver(player1.aid);
			send(msg);

			gui.logLine("Main Waiting for movement");
			ACLMessage move1 = blockingReceive();
			gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
			pos1 = Integer.parseInt(move1.getContent().split("#")[1]);

			msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setContent("Position");
			msg.addReceiver(player2.aid);
			send(msg);

			gui.logLine("Main Waiting for movement");
			ACLMessage move2 = blockingReceive();
			gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
			pos2 = Integer.parseInt(move1.getContent().split("#")[1]);

			rewardP1 = matrix.getRewardP1(pos1, pos2);
			rewardP2 = matrix.getRewardP2(pos1, pos2);
			
			p1Score += rewardP1;
			p2Score += rewardP2;
			
			msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(player1.aid);
			msg.addReceiver(player2.aid);
			msg.setContent("Results#" + pos1 + "," + pos2 + "#" + rewardP1 + "," + rewardP2);
			send(msg);
			
			msg.setContent("EndGame");
			send(msg);
		}

		@Override
		public boolean done() {
			return true;
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
		
	}
}
