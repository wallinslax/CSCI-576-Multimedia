
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ImageDisplay {
	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 1920; // default image width and height
	int height = 1080;
	int newWidth, newHeight;
	String imgPath;
	int subSamplingY,subSamplingU,subSamplingV;
	float Sw,Sh;
	int isAntialiasing;

	// imgPath,subSamplingY,subSamplingU,subSamplingV,Sw,Sh,A
	public ImageDisplay(String imgPath, int subSamplingY1, int subSamplingU, int subSamplingV,float Sw, float Sh,int isAntialiasing) {
        assert subSamplingY1 >= 1: "subsampling for Y should greater than or equal to 1";
        assert subSamplingU >= 1: "subsampling for U should greater than or equal to 1";
        assert subSamplingV >= 1: "subsampling for V should greater than or equal to 1";
		assert 1 >= Sw && Sw>= 0: "Should only small the picture";
		assert 1 >= Sh && Sh>= 0: "Should only small the picture";
		assert isAntialiasing == 1 || isAntialiasing == 0 : "isAntialiasing should be either 0 or 1";
        this.imgPath = imgPath;
        this.subSamplingY = subSamplingY1;
        this.subSamplingU = subSamplingU;
        this.subSamplingV = subSamplingV;
		this.Sw = Sw;
		this.Sh = Sh;
		this.isAntialiasing = isAntialiasing;
		this.newWidth = (int) Math.round(width * Sw);
        this.newHeight = (int) Math.round(height * Sh);
    }

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void subSampling(double[][] pixels, int subBy){
		for(int y = 0; y<height; y++)
			for(int x=0; x<width; x++){
				int dist = x % subBy;
				if (dist != 0 && x +  (subBy - dist) < width ){
					Double left = (subBy - dist) * pixels[y][x-dist];
					Double right = dist * pixels[y][x + (subBy - dist)];
					pixels[y][x] = (left+right)/subBy;
				}
			}

	}

	private int double2int(double num){
		if (num > 255) num = 255;
		if (num < 0) num = 0;
		// if (num > 127) num = 127;
		// if (num < -128) num = -128;
		int byteNum = (int) Math.round(num);
		return byteNum;
	}

	private int[][] blur(int[][] matrix){
		int[][] newMatrix = new int[height][width];
		for(int y = 0; y<height; y++)
			for(int x = 0; x<width; x++){
				int sum = 0;
				int count = 0;
				// 3*3 kernel
				for (int yNei = y-1; yNei < y+2; yNei++) {
					for (int xNei = x-1; xNei < x+2; xNei++) {
						if (0 <= yNei && yNei < height && 0 <= xNei && xNei < width) {
							count += 1;
							sum += matrix[yNei][xNei];
						}
					}
				}
				newMatrix[y][x] = double2int(sum/count);
			}
		return newMatrix;
	}

	private void readImageRGB()
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);
			// 1. RGB domain to YUV domain
			double[][] pixelsY = new double[height][width];
			double[][] pixelsU = new double[height][width];
			double[][] pixelsV = new double[height][width];
			int[][] orgPixels = new int[height][width];
			int ind = 0;
			for(int y = 0; y < height; y++)
				for(int x = 0; x < width; x++){
					byte a = 0;
					int r = bytes[ind] & 0xff;
					int g = bytes[ind+height*width] & 0xff;
					int b = bytes[ind+height*width*2] & 0xff; 
					if (r<0 || g<0 || b<0)
						System.out.printf("lala");
					ind++;
					int pixel = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					orgPixels[y][x] = pixel;
					// System.out.println("pixel="+ ind);
					// System.out.println("r="+ r);
					// System.out.println("g="+ g);
					// System.out.println("b="+ b);
					
					pixelsY[y][x] = 0.299*r + 0.587*g + 0.114*b;
					pixelsU[y][x] = 0.596*r + (-0.274)*g + (-0.322)*b;
 					pixelsV[y][x] = 0.211*r + (-0.523)*g + (0.312)*b;					
				}
			// 2. Subsampling	
			subSampling(pixelsY, subSamplingY);
			subSampling(pixelsU, subSamplingU);
			subSampling(pixelsV, subSamplingV);

			// 3. convert subsampled YUV back to RGB in orignal size
			int[][] orgSizeSub_r = new int[height][width];
			int[][] orgSizeSub_g = new int[height][width];
			int[][] orgSizeSub_b = new int[height][width];
			for(int y = 0; y<height; y++)
				for(int x=0; x<width; x++){
					double pre_r = 1*pixelsY[y][x] + 0.956*pixelsU[y][x] + 0.621*pixelsV[y][x];
					double pre_g = 1*pixelsY[y][x] + (-0.272)*pixelsU[y][x] + (-0.647)*pixelsV[y][x];
					double pre_b = 1*pixelsY[y][x] + (-1.106)*pixelsU[y][x] + 1.703*pixelsV[y][x];
					int sub_r = double2int(pre_r);
					int sub_g = double2int(pre_g);
					int sub_b = double2int(pre_b);
					orgSizeSub_r[y][x] = sub_r;
					orgSizeSub_g[y][x] = sub_g;
					orgSizeSub_b[y][x] = sub_b;
					// img.setRGB(x,y,subPix);
				}
			// 4. [optional] blur (antialias)
			int[][] orgSizeSubBlur_r = blur(orgSizeSub_r);
			int[][] orgSizeSubBlur_g = blur(orgSizeSub_g);
			int[][] orgSizeSubBlur_b = blur(orgSizeSub_b);

			// 5. construct original size/subsampled/[optional] blured/RGB in pixel
			int[][] orgSizeSubBlurPix = new int[height][width];
			for(int y = 0; y<height; y++)
				for(int x=0; x<width; x++){
					int sub_r,sub_g,sub_b;
					if(isAntialiasing==1){
						sub_r = orgSizeSubBlur_r[y][x];
						sub_g = orgSizeSubBlur_g[y][x];
						sub_b = orgSizeSubBlur_b[y][x];
					}else{
						sub_r = orgSizeSub_r[y][x];
						sub_g = orgSizeSub_g[y][x];
						sub_b = orgSizeSub_b[y][x];
					}
					int pixel = 0xff000000 | ((sub_r & 0xff) << 16) | ((sub_g & 0xff) << 8) | (sub_b & 0xff);
					orgSizeSubBlurPix[y][x] = pixel;

				}

			// 6. Resize
			// BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
			imgOne = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
			for(int y = 0; y<newHeight; y++)
				for(int x=0; x<newWidth; x++){ 
					int orgX = (int) Math.round(x/Sw);
					int orgY = (int) Math.round(y/Sh);
					// imgOne.setRGB(x, y, orgPixels[orgY][orgX]);
					imgOne.setRGB(x, y, orgSizeSubBlurPix[orgY][orgX]);
				}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void showIms(){
		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));

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
		// System.out.println("lala");
		try
        {
			String[] parts = imgPath.split("/");
			String fileName = parts[parts.length - 1];
            ImageIO.write(imgOne,"jpeg", new File("img/"+ fileName + "_Y_" + subSamplingY + "_U_"+subSamplingU + "_V_"
			+subSamplingV + "_Sw_"+ Sw + "_Sh_" + Sh + "_A_" + isAntialiasing + ".jpg"));
        }
        catch(Exception exception)
        {
            System.out.println("exception ImageIO");
        }
	}

	public static void main(String[] args) {
		String imgPath = args[0];
		int subSamplingY = Integer.parseInt(args[1]);
        int subSamplingU = Integer.parseInt(args[2]);
        int subSamplingV = Integer.parseInt(args[3]);
		float Sw = Float.parseFloat(args[4]);
		float Sh = Float.parseFloat(args[5]);
		int A =  Integer.parseInt(args[6]);
		
		// String imgPath = "img/lake-forest_1920w_1080h.rgb";
		// String imgPath2 = "img/miamibeach.rgb";
		// int subSamplingY = 1;
        // int subSamplingU = 1;
        // int subSamplingV = 1;
		// float Sw = 1f;
		// float Sh = 1f;
		// int A =  0;

		ImageDisplay ren = new ImageDisplay(imgPath,subSamplingY,subSamplingU,subSamplingV,Sw,Sh,A);
		ren.readImageRGB();
		ren.showIms();
	}

}
