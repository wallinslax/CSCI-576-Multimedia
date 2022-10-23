import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.lang.Exception;
import java.lang.Thread;
import java.util.*;

public class JPEG2000 {
	String rgbPath;
	int width, height; // default image width and height
	int targetLevel = 8;
	// img
	BufferedImage inImg;
	BufferedImage[] outImgs;
	double[][] inPixelsY, inPixelsCr, inPixelsCb, outPixelsY, outPixelsCr, outPixelsCb;

	public JPEG2000(String rgbPath,int mode,int width,int height) {
		assert width> 0: "Width should be positive int";
		assert height> 0: "Width should be positive int";
		assert mode <= 9 || -1 <= mode : "mode should be integar between -1 to 9";
		// getPaths
		this.rgbPath = rgbPath;
		this.width = width;
		this.height = height;
		this.targetLevel = mode;
		// Y Cr Cb channel 
		inPixelsY = new double[height][width];
		inPixelsCr = new double[height][width];
		inPixelsCb = new double[height][width];
		outPixelsY = new double[height][width];
		outPixelsCr = new double[height][width];
		outPixelsCb = new double[height][width];
		// read image
		inImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(rgbPath, inImg);
		outImgs = new BufferedImage[10];
		if(this.targetLevel==-1)
			for(int i=0; i<10; i++)
				process(i);
		else
			process(this.targetLevel);

	}

	private double[][] DWT(double[][] data, int level){
		int width = (int) Math.pow(2, level);
		int height = (int) Math.pow(2, level);
		double[][] tmp = new double[height][width];
		
		for(int y = 0; y < height; y++){// horizon compress
			for(int x = 0; x < width; x += 2) // lowpass
				tmp[y][x/2] = (data[y][x] + data[y][x+1])/2;
			for(int x = 0; x < width; x += 2) // highpass
				tmp[y][(width/2) + x/2] = (data[y][x] - data[y][x+1])/2;
		}
		for(int y = 0; y < height; y++)
			for(int x = 0; x < width; x++)
				data[y][x] = tmp[y][x];
		tmp = new double[height][width];
		for(int x = 0; x < width; x++){// vertical compress
			for(int y = 0; y < height; y += 2) // lowpass
				tmp[y/2][x] = (data[y][x] + data[y+1][x])/2;
			for(int y = 0; y < height; y += 2) // highpass
				tmp[height/2 + y/2][x] = (data[y][x] - data[y+1][x])/2;
		}
		for(int y = 0; y < height; y++)
			for(int x = 0; x < width; x++)
				data[y][x] = tmp[y][x];
		return data;
	}
	
	private double[][] iDWT(double[][] data, int level){ // given current level return next level
		int width = (int) Math.pow(2, level);
		int height = (int) Math.pow(2, level);
		double[][] tmp = new double[height*2][width*2];
		for(int x = 0; x < width*2; x++){// vertical compress
			for(int y = 0; y < height; y++){ // lowpass
				tmp[2*y][x] = data[y][x] + data[y + height][x];
				tmp[2*y+1][x] = data[y][x] - data[y + height][x];
			}
		}
		for(int y = 0; y < height*2; y++)
			for(int x = 0; x < width*2; x++)
				data[y][x] = tmp[y][x];

		tmp = new double[height*2][width*2];
		for(int y = 0; y < height*2; y++){// horizon compress
			for(int x = 0; x < width; x++){// lowpass
				tmp[y][2*x] = data[y][x] + data[y][x+width];
				tmp[y][2*x+1] = data[y][x] - data[y][x+width];
			}	
		}
		
		for(int y = 0; y < height*2; y++)
			for(int x = 0; x < width*2; x++)
				data[y][x] = tmp[y][x];
		return data;
	}

	private int double2int(double num){
		if (num > 255) num = 255;
		if (num < 0) num = 0;
		// if (num > 127) num = 127;
		// if (num < -128) num = -128;
		int byteNum = (int) Math.round(num);
		return byteNum;
	}
	
	private double[][] killCoef(double[][] data, int level){
		int width = (int) Math.pow(2, level);
		int height = (int) Math.pow(2, level);
		double[][] tmp = new double[this.height][this.width];
		for(int y = 0; y < height*2; y++)
			for(int x = 0; x < width*2; x++)
				tmp[y][x] = 0;
		for(int y = 0; y < height; y++)
			for(int x = 0; x < width; x++)
				tmp[y][x] = data[y][x];
		return tmp;
	}

	private void matrixCopy(double[][] inPixelsY, double[][] outPixelsY){
		for(int y = 0; y < this.height; y++)
			for(int x = 0; x < this.width; x++)
				outPixelsY[y][x] = inPixelsY[y][x];
	}

	private void process(int givenLevel){
		// Discrete Wavelet Transform (DWT)
		// double[][] a =  { {9,7,3,5} , {9,7,3,5},{9,7,3,5},{9,7,3,5} };
		// a = DWT(a, 2);
		// a = iDWT(a, 1);
		matrixCopy(inPixelsY, outPixelsY);
		matrixCopy(inPixelsCb, outPixelsCb);
		matrixCopy(inPixelsCr, outPixelsCr);
		for(int level = 9; level > givenLevel; level--){
			outPixelsY = DWT(outPixelsY, level);
			outPixelsCb = DWT(outPixelsCb, level);
			outPixelsCr = DWT(outPixelsCr, level);
		}
		if(givenLevel < 9){
			outPixelsY = killCoef(outPixelsY, givenLevel);
			outPixelsCb = killCoef(outPixelsCb, givenLevel);
			outPixelsCr = killCoef(outPixelsCr, givenLevel);
		}
		// Inverse DWT
		for(int level = givenLevel; level<9; level++){
			outPixelsY = iDWT(outPixelsY, level);
			outPixelsCb = iDWT(outPixelsCb, level);
			outPixelsCr = iDWT(outPixelsCr, level);
		}

		// 3. YCrCb -> RGB 
		BufferedImage hahaImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);;
		for(int y = 0; y < height; y++)
			for(int x = 0; x < width; x++){
				double pre_r = 1*outPixelsY[y][x] + 0*outPixelsCb[y][x] + 1.4021*outPixelsCr[y][x];
				double pre_g = 1*outPixelsY[y][x] + (-0.3441)*outPixelsCb[y][x] + (-0.7142)*outPixelsCr[y][x];
				double pre_b = 1*outPixelsY[y][x] + (1.7718)*outPixelsCb[y][x] + 0*outPixelsCr[y][x];
				int sub_r = double2int(pre_r);
				int sub_g = double2int(pre_g);
				int sub_b = double2int(pre_b);
				int pixel = 0xff000000 | ((sub_r & 0xff) << 16) | ((sub_g & 0xff) << 8) | (sub_b & 0xff);
				hahaImg.setRGB(x,y,pixel);
			}
		showIms(hahaImg);
	}

	private void readImageRGB(String imgPath, BufferedImage img){
		File file = new File(imgPath);
		try{
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);
			int frameLength = width*height*3;
			long len = frameLength;
			byte[] bytes = new byte[(int) len];
			raf.read(bytes);
			raf.close();
			int ind = 0;
			for(int y = 0; y < height; y++)
				for(int x = 0; x < width; x++){
					int r = bytes[ind] & 0xff;
					int g = bytes[ind+height*width] & 0xff;
					int b = bytes[ind+height*width*2] & 0xff;
					ind++; 
					int pixel = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					img.setRGB(x, y, pixel);
					// RGB -> YCrCb	
					// https://www.imaging.org/site/PDFS/Papers/2000/PICS-0-81/1645.pdf
					inPixelsY[y][x] = 0.299*r + 0.587*g + 0.114*b;
					inPixelsCb[y][x] = (-0.169)*r + (-0.331)*g + (0.5)*b;
					inPixelsCr[y][x] = 0.5*r + (-0.419)*g + (-0.081)*b;	
				}
		}
		catch (FileNotFoundException e){
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			// catching the exception
			System.out.println(e);
		}
		// showIms(img);
		// showIms(img);
	}

	public void showIms(BufferedImage imgOne){
		// Use label to display the image
		JFrame frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbIm1 = new JLabel(new ImageIcon(imgOne));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {	
		String rgbPath = args[0];
        int level = Integer.parseInt(args[1]);
		// String rgbPath = "lake-forest_512_512.rgb";
        // int level = -1;

		int width = 512;
		int height = 512;
		JPEG2000 pkg1 = new JPEG2000(rgbPath,level,width,height);
		// pkg1.showImg(null);
	}

}
