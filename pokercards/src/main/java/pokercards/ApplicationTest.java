package pokercards;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class ApplicationTest {
	private static final String CHECK = "check";
	private static final int CHAR_HEIGHT = 50;
	private static final int CHAR_WIDTH = 30;
	private static final int CARD_WIDTH = 70;
	private static final int CARD_HEIGHT = 90;
	private static final int CENTERX = 140;
	private static final int CENTERY = 585;
	private static final String SEPARATOR = "/";
	private static final String NOT_FOUND = "_";
	private static final int ARRAY_GAP = 3;
	private static final int VALUE_GAP = 50;
	private static final int BG_VALUE = 50;
	
	private static boolean DEBUG_MODE = false;

	public static void main(String[] args) {
		//scan(new File(args[0]), 8, 2, "Qs");
		//DEBUG_MODE = true; check(new File(args[0]), 43, 2, "Qc");
		Date startDate = new Date();
		check(new File(args[0]), -1, -1, null);
		System.out.println("total time elapsed:"+(new Date().getTime()-startDate.getTime()));
		System.out.println((new Date().getTime()-startDate.getTime())/new File(args[0]).listFiles().length+" ms per picture");
	}
	
	public static void read(File pictures) {
		int found = 0;
		try (BufferedReader readerCorrect = new BufferedReader(new FileReader("D:\\DIFFERENT\\java_test_task\\correctResults.txt"));BufferedReader readerResults = new BufferedReader(new FileReader("D:/results.txt"));){
			String line = null;
			while ((line = readerCorrect.readLine()) != null) {
				if (line.equals(readerResults.readLine())) found++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Recognized "+found+" from "+pictures.listFiles().length+" pictures. "+(pictures.listFiles().length-found)+" pictures are left.");
	}
	
	public static void check(File pictures, int pictureNumber, int cardNumber, String checkFileName) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("d:/results.txt", DEBUG_MODE))){
			int pnumber = 0;
			for (File picture:pictures.listFiles()) {
				pnumber++;
				if (pictureNumber != -1 && pnumber != pictureNumber) continue;
				
				Date startDate = new Date();
				
				String cards[] = new String[]{NOT_FOUND, NOT_FOUND, NOT_FOUND, NOT_FOUND, NOT_FOUND};
				
				BufferedImage image = ImageIO.read(picture);
				check(image, cards, cardNumber, 0, checkFileName);
				check(image, cards, cardNumber, BG_VALUE, checkFileName);
				
				String result = String.format("%-3s: %s - %s %s %s %s %s", pnumber, picture.getAbsolutePath(), cards[0], cards[1], cards[2], cards[3], cards[4]).trim();
				if (!DEBUG_MODE) {
					writer.write(result);
					if (pnumber != pictures.listFiles().length) writer.write("\n");
				}
				System.out.println(result+" time elapsed:"+(new Date().getTime()-startDate.getTime()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		read(pictures);
	}
	
	private static void check(BufferedImage image, String cards[],  int cardNumber, int bgValue, String checkFileName) throws Exception {
		for (String checkFile:getCheckFiles()) {
			if (checkFileName != null && !checkFileName.equals(checkFile)) continue;
			
			for (int cnumber=0;cnumber<cards.length;cnumber++) {
				if (cardNumber != -1 && cardNumber != cnumber) continue;
				if (cards[cnumber].equals(NOT_FOUND) && check(image, checkFile, cnumber, bgValue) != -1) {
					cards[cnumber]=checkFile.substring(checkFile.lastIndexOf(SEPARATOR)+1);
				}
			}
		}
	}
	
	private static List<String> getCheckFiles() throws Exception{
		if (ApplicationTest.class.getResource(SEPARATOR+CHECK).toURI().getScheme().contains("jar")) {
			URL jar = ApplicationTest.class.getProtectionDomain().getCodeSource().getLocation();
			Path jarFile = Paths.get(jar.toString().substring("file:/".length()));
			
			List<String> result = new ArrayList<String>();
			Files.newDirectoryStream(FileSystems.newFileSystem(jarFile).getPath(CHECK)).forEach(path->{
				result.add(path.toString());
			});
			return result;
		}
		
		StringWriter result = new StringWriter();
		try (InputStream stream = (InputStream)ApplicationTest.class.getResource(SEPARATOR+CHECK).getContent()){
			int numRead;
	        byte[] buffer = new byte[10*1024];

	        while ((numRead = stream.read(buffer)) != -1) {
	        	result.write(new String(buffer), 0, numRead);
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Arrays.asList(result.toString().split("\n")).stream().map(checkFileName->SEPARATOR+CHECK+SEPARATOR+checkFileName).collect(Collectors.toList());
	}
	
	public static void scan(File pictures, int pictureNumber, int cardNumber, String checkFileName) {
		int number = 0;
		for (File picture:pictures.listFiles()) {
			String fileName = CHECK+SEPARATOR+checkFileName;
			number++;
			if (pictureNumber != -1 && number != pictureNumber) continue;
			
			try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))){
				BufferedImage img = ImageIO.read(picture).getSubimage(150+CARD_WIDTH*(cardNumber-1), 590, CHAR_WIDTH, CHAR_HEIGHT);//char
				
				int [][] matrix = new int[img.getHeight()][img.getWidth()];
				for (int y=0;y<img.getHeight();y++) {
					for (int x=0;x<img.getWidth();x++) {
						matrix[y][x] = img.getRGB(x, y);
					}
				}
				
				out.writeObject(matrix);
				printMatrix(matrix);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static int check(BufferedImage image, String checkFileName, int cardNumber, int bgValueGap) {
		try (ObjectInputStream in = new ObjectInputStream(ApplicationTest.class.getResourceAsStream(checkFileName))) {
			int [][] checkMatrix = (int [][])in.readObject();
			if (DEBUG_MODE) printMatrix(checkMatrix);
			
			BufferedImage card = image.getSubimage(CENTERX+CARD_WIDTH*cardNumber, CENTERY, CARD_WIDTH, CARD_HEIGHT);//center card image by number 
			
			int mainLine=0;
			int checkLine = 0;
			int prevIndex = 0;

			while (mainLine<card.getHeight()) {
				int mainArray[] = new int[card.getWidth()];
				for (int x=0;x<card.getWidth();x++) mainArray[x] = card.getRGB(x, mainLine);
				if (DEBUG_MODE) System.out.print(String.format("%-3s: ", mainLine));
				if (DEBUG_MODE) printArray(mainArray, bgValueGap);
				
				int index = compareArrays(mainArray, checkMatrix[checkLine], bgValueGap);
				if (index != -1) {
					if (DEBUG_MODE) System.out.print(String.format("%-"+4*(index == 0?1:index)+"s%-3s: ", " ", checkLine));
					if (DEBUG_MODE) printArray(checkMatrix[checkLine], 0);
					
					checkLine++; mainLine++;
					
					if (checkLine == checkMatrix.length) return mainLine-checkLine;
				} else {
					if (DEBUG_MODE) System.out.print(String.format("%-"+4*(prevIndex <= 0?1:prevIndex+1)+"s ", " "));
					if (DEBUG_MODE) printArray(checkMatrix[checkLine], 0);
					
					mainLine = mainLine - checkLine+1;
					checkLine = 0;
				}
				
				prevIndex = index;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return -1;
	}
	
	private static int compareArrays(int mainArray[], int subArray[], int bgValueGap) {
		int mainIndex=0;
		int subIndex=0;
		int allowedGap = ARRAY_GAP;
		
		while (mainIndex<mainArray.length && subIndex<subArray.length) {
			int valueGap = Math.abs(getColorValue(new Color(mainArray[mainIndex]))-bgValueGap-getColorValue(new Color(subArray[subIndex])));
			
			if (valueGap<=VALUE_GAP || allowedGap-->0) {
				mainIndex++; subIndex++;

				if (subIndex == subArray.length) {
					return mainIndex-subIndex;
				}
			} else {
				mainIndex = mainIndex - subIndex + 1;
				subIndex=0;
				allowedGap=ARRAY_GAP;
			}
		}

		return -1;
	}
	
	public static void printMatrix(int [][] checkMatrix) {
		for (int y=0;y<checkMatrix.length;y++) {
			if (y == 0) {
				System.out.print(String.format("%-5s", "CMX"));
				for (int x=0;x<checkMatrix[y].length;x++) {
					System.out.print(String.format("%-3s ", x));
				}
				System.out.println();
			}
			System.out.print(String.format("%-3s: ", y));
			printArray(checkMatrix[y], 0);
		}
		System.out.println("====================================");
	}
	
	public static void printArray(int array[], int bgValueGap) {
		for (int i=0;i<array.length;i++) {
			System.out.print(String.format("%-3s ", getColorValue(new Color(array[i]))-bgValueGap));
		}
		System.out.println();
	}
	
	public static int getColorValue(Color color) {
		int devide = 5;
		return (255-color.getRed())/devide+(255-color.getGreen())/devide+(255-color.getBlue())/devide;
	}
}