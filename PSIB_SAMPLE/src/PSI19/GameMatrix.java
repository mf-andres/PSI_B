package PSI19;
import java.util.ArrayList;

public class GameMatrix {

	int s;
	int[][][] matrix;
	
	GameMatrix(int s){
	
		this.s = s;
		matrix = new int[s][s][2];
		fillMatrix();
	}
	
	int getRewardP1(int row, int column) {
		
		return matrix[row][column][0];
	}
	
	void setRewardP1(int row, int column, int value) {
		
		matrix[row][column][0] = value;
	}
	
	int getRewardP2(int row, int column) {
		
		return matrix[row][column][1];
	}
	
	void setRewardP2(int row, int column, int value) {
		
		matrix[row][column][1] = value;
	}
	
	void fillMatrix(){
		
		for(int i = 0;i < s; i++) {

			fillCorner(i);
		}	
	}
	
	void fillCorner(int level){
		
		for(int i = level; i < s; i++) {
			
			int p1Reward = getRandomReward();
			int p2Reward = getRandomReward();
			
			//to fill the row
			setRewardP1(level, i, p1Reward);
			setRewardP2(level, i, p2Reward);
			
			//to fill the column symmetrically
			setRewardP1(i, level, p2Reward);
			setRewardP2(i, level, p1Reward);
		}
	}
	
	int getRandomReward() {
		
		return getRandom(9);
	}
	
	int getRandom(int max) {
		
		int random = (int) (Math.random() * max); 
		return random;
	}
	
	float alter(float p) {
		
		int numberOfCells = s * s;
		float altered = 0;
		ArrayList<int[]> alteredCells = new ArrayList<int[]>();
		
		while(altered < p) {
			
			int[] cell = getRandomCell(s);
			if(alteredCells.contains(cell)) {
				
				continue;
				
			} else {
				
				alterCell(cell);
				alteredCells.add(cell);
				
				//to keep symmetry
				if(cell[0] != cell [1]) {
					
					int[] antiCell = {cell[1],  cell[0]};
					alteredCells.add(antiCell);
				}
				
				altered = (float) alteredCells.size() / (float) numberOfCells;
			}
		}
		
		return altered;
	}
	
	void alterCell(int[] cell) {
		
		int row = cell[0];
		int column = cell[1];
		int value1 = getRandomReward();
		int value2 = getRandomReward();
		
		setRewardP1(row, column, value1);
		setRewardP2(row, column, value2);
		
		//to keep the symmetry
		if(row != column) {
		
			setRewardP1(column, row, value2);
			setRewardP2(column, row, value1);			
		}
	}
	
	int[] getRandomCell(int s) {
		
		int row = getRandom(s);
		int column = getRandom(s);
		int[] cell = {row, column};
		
		return cell;
	}
	
	String[][] getTranslated(){
		
		String[][] traductedMatrix = new String[s][s];
		
		for(int i = 0; i<s; i++) {
			for(int j=0; j<s; j++) {
				
				int rp1 = this.getRewardP1(i, j);
				int rp2 = this.getRewardP2(i, j);
				traductedMatrix[i][j] = "(" + rp1 + "," + rp2 + ")";
			}
		}
		
		return traductedMatrix;
	}
	
	String[] getColumnNames() {
		
		String[] columnNames = new String[s];
		
		for(int i = 0; i < s; i++) {
			
			columnNames[i] = "*";
		}
		
		return columnNames;
	}
}
