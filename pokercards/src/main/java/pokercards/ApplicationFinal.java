package pokercards;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class ApplicationFinal {
	private static final String SEPARATOR = "/";
	private static final String CHECK = "check";
	private static final String RESULT = "%s - %s%s%s%s%s";
	private static final int CARD_WIDTH = 70;
	private static final int CARD_HEIGHT = 90;
	private static final int CENTERX = 140;
	private static final int CENTERY = 585;
	private static final int ARRAY_GAP = 3;
	private static final int VALUE_GAP = 50;
	private static final int BG_VALUE = 50;

	public static void main(String[] args) {
		if (args.length == 0 || args[0] == null || args[0].isEmpty()) {
			System.out.println("Please specify path to the pictures!");
			return;
		}
		
		check(new File(args[0]));
	}
	
	private static void check(File pictures) {
		if (pictures.listFiles() == null) {
			System.out.println("Path to the pictures is incorrect. Please specify correct path to the pictures!");
			return;
		}
		
		for (File picture:pictures.listFiles()) {
			String cards[] = new String[]{"", "", "", "", ""};
			
			try {
				BufferedImage image = ImageIO.read(picture);
				check(image, cards, 0);
				check(image, cards, BG_VALUE);
			} catch (Exception e) {
				System.out.println("Can't scan "+picture.getAbsolutePath()+": "+e.getMessage());
			}
			
			System.out.println(String.format(RESULT, picture.getAbsolutePath(), cards[0], cards[1], cards[2], cards[3], cards[4]));
		}
	}
	
	private static void check(BufferedImage image, String cards[], int bgValue) throws Exception{
		for (String checkFileName:getCheckFiles()) {
			for (int cnumber=0;cnumber<cards.length;cnumber++) {
				if (cards[cnumber].isEmpty() && check(image, checkFileName, cnumber, bgValue) != -1) {
					cards[cnumber]=checkFileName.substring(checkFileName.lastIndexOf(SEPARATOR)+1);
				}
			}
		}
	}
	
	private static List<String> getCheckFiles() throws Exception {
		if (ApplicationTest.class.getResource(SEPARATOR+CHECK).toString().startsWith("jar")) {
			URL jar = ApplicationTest.class.getProtectionDomain().getCodeSource().getLocation();
			Path jarFile = Paths.get(jar.toString().substring("file:/".length()));
			
			List<String> result = new ArrayList<String>();
			Files.newDirectoryStream(FileSystems.newFileSystem(jarFile, null).getPath(CHECK)).forEach(path->{
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
		}

		return Arrays.asList(result.toString().split("\n")).stream().map(checkFileName->SEPARATOR+CHECK+SEPARATOR+checkFileName).collect(Collectors.toList());
	}
	
	private static int check(BufferedImage image, String checkFileName, int cardNumber, int bgValueGap) throws Exception{
		try (ObjectInputStream in = new ObjectInputStream(ApplicationTest.class.getResourceAsStream(checkFileName))) {
			int [][] checkMatrix = (int [][])in.readObject();
			
			BufferedImage card = image.getSubimage(CENTERX+CARD_WIDTH*cardNumber, CENTERY, CARD_WIDTH, CARD_HEIGHT);//center card image by number 
			
			int mainLine=0;
			int checkLine = 0;

			while (mainLine<card.getHeight()) {
				int mainArray[] = new int[card.getWidth()];
				for (int x=0;x<card.getWidth();x++) mainArray[x] = card.getRGB(x, mainLine);
				
				int index = compareArrays(mainArray, checkMatrix[checkLine], bgValueGap);
				if (index != -1) {
					checkLine++; mainLine++;
					
					if (checkLine == checkMatrix.length) return mainLine-checkLine;
				} else {
					mainLine = mainLine - checkLine+1;
					checkLine = 0;
				}
			}
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

				if (subIndex == subArray.length) return mainIndex-subIndex;
			} else {
				mainIndex = mainIndex - subIndex + 1;
				subIndex=0;
				allowedGap=ARRAY_GAP;
			}
		}

		return -1;
	}
	
	private static int getColorValue(Color color) {
		int devide = 5;
		return (255-color.getRed())/devide+(255-color.getGreen())/devide+(255-color.getBlue())/devide;
	}
}