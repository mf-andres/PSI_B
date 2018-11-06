package PSI19;

public class ScoreMatrix {

	int[] matrix;
	
	public ScoreMatrix(int n) {
		
		matrix = new int[n];
	}

	public int getScore(int playerId) {
		
		return matrix[playerId];
	}
	
	public void updateScore(int playerID, int score) {
		
		matrix[playerID] += score;
	}
	
	public String toString() {
		
		String str = "";
		for(int score : matrix) {
			
			str = str + ", " + Integer.toString(score);
		}
		
		return str;
	}
	
	public int[] getMatrix() {
		
		return matrix;
	}
}
