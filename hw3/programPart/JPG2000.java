
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.ArrayList;


public class JPG2000 {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 512; // default image width and height
	int height = 512;
	// Constants as used by Gregoire P.
	double alpha = -1.586134342;
	double beta = -0.05298011854;
	double gamma = 0.8829110762;
	double delta = 0.4435068522;
	double zeta = 1.149604398;
	int N = width;
	Boolean naive = false; //execute what teacher said in the class
	ArrayList<Double>ICT = new ArrayList<>(); //Irreversible Color Transform
	ArrayList<double[][]>ictY = new ArrayList<>(); //Irreversible Color Transform
	ArrayList<double[][]>ictCb = new ArrayList<>();
	ArrayList<double[][]>ictCr = new ArrayList<>();
	double x[];
	double Y [];
	double yy[][];
	double Cb [];
	double cb[][];
	double Cr [];
	double cr[][];
	double matrix[][] = {
		{0.299, 0.587, 0.114},
		{-0.16875, -0.33126, 0.5},
		{0.5, -0.41869, -0.08131}
	};
	double inverse[][] = {
		{1, 0, 1.402},
		{1.0, -0.34413, -0.71414},
		{1.0, 1.772, 0.0}
	};
	//https://web.archive.org/web/20120305164605/http://www.embl.de/~gpau/misc/dwt97.c
	//https://github.com/accord-net/framework/blob/master/Sources/Accord.Math/Wavelets/CDF97.cs#L103
	/**
	 *  fwt97 - Forward biorthogonal 9/7 wavelet transform (lifting implementation)
	 *
	 *  x is an input signal, which will be replaced by its output transform.
	 *  n is the length of the signal, and must be a power of 2.
	 *
	 *  The first half part of the output signal contains the approximation coefficients.
	 *  The second half part contains the detail coefficients (aka. the wavelets coefficients).
	 *
	 *  See also iwt97.
	 */
	double[][] fwt2d(double[][] x, int width, int height)
	{
		//System.out.println(width+" : " + x.length);
		for (int j = 0; j < width; j++)
		{
			// Predict 1
			for (int i = 1; i < height - 1; i += 2)
				x[i][j] += alpha * (x[i - 1][j] + x[i + 1][j]);
			x[height - 1][j] += 2 * alpha * x[height - 2][j];

			// Update 1
			for (int i = 2; i < height; i += 2)
				x[i][j] += beta * (x[i - 1][j] + x[i + 1][j]);
			x[0][j] += 2 * beta * x[1][j];

			// Predict 2
			for (int i = 1; i < height - 1; i += 2)
				x[i][j] += gamma * (x[i - 1][j] + x[i + 1][j]);
			x[height - 1][j] += 2 * gamma * x[height - 2][j];

			// Update 2
			for (int i = 2; i < height; i += 2)
				x[i][j] += delta * (x[i - 1][j] + x[i + 1][j]);
			x[0][j] += 2 * delta * x[1][j];
		}

		// Pack
		var tempbank = new double[width][height];
		for (int i = 0; i < height; i++)
		{
			for (int j = 0; j < width; j++)
			{
				if ((i % 2) == 0)
					tempbank[j][i / 2] = (1 / zeta) * x[i][j];
				else
					tempbank[j][ i / 2 + height / 2] = (zeta / 2) * x[i][j];
			}
		}

		for (int i = 0; i < width; i++)
			for (int j = 0; j < width; j++)
				x[i][j] = tempbank[i][j];

		return x;
	}

	double[][] iwt2d(double[][] x, int width, int height)
	{
		//System.out.println(width+" : " + x.length);
		// Unpack
		var tempbank = new double[width][height];
		for (int j = 0; j < width / 2; j++)
		{
			for (int i = 0; i < height; i++)
			{
				tempbank[j * 2][i] = zeta * x[i][j];
				tempbank[j * 2 + 1][ i] = (2 / zeta) * x[i][ j + width / 2];
			}
		}

		for (int i = 0; i < width; i++)
			for (int j = 0; j < height; j++)
				x[i][ j] = tempbank[i][j];


		for (int j = 0; j < width; j++)
		{
			// Undo update 2
			for (int i = 2; i < height; i += 2)
				x[i][ j] -= delta * (x[i - 1][ j] + x[i + 1][j]);
			x[0][ j] -= 2 * delta * x[1][j];

			// Undo predict 2
			for (int i = 1; i < height - 1; i += 2)
				x[i][j] -= gamma * (x[i - 1][ j] + x[i + 1][ j]);
			x[height - 1][ j] -= 2 * gamma * x[height - 2][ j];

			// Undo update 1
			for (int i = 2; i < height; i += 2)
				x[i][ j] -= beta * (x[i - 1][ j] + x[i + 1][ j]);
			x[0][j] -= 2 * beta * x[1][ j];

			// Undo predict 1
			for (int i = 1; i < height - 1; i += 2)
				x[i][ j] -= alpha * (x[i - 1][ j] + x[i + 1][ j]);
			x[height - 1][ j] -= 2 * alpha * x[height - 2][ j];
		}

		return x;
	}
	//naive encode
	double[][] encode(double[][] x, int width, int height) {
		//System.out.println(width+" : " + x.length);
		//do row first
		//temp = x;
		int mid = width/2;
		double[][] temp = new double[height][width];
		double[][] tmp = new double[height][width];
		for (int i = 0; i < height; i++) {
			//L pass filter
			for (int j = 1, k = 0; j < width; j+=2, k++) {
				temp[i][k] = (x[i][j]+x[i][j-1])/2;
			}
			//H pass filter
			for (int j = 1, k = mid; j < width; j+=2, k++) {
				temp[i][k] = (x[i][j-1]-x[i][j])/2;
			}
		}
		//do column
		for (int j = 0; j < width; j++) {
			//L pass filter
			for (int i = 1, k = 0; i < height; i+=2, k++) {
				tmp[k][j] = (temp[i][j]+temp[i-1][j])/2;
			}
			//H pass filter
			for (int i = 1, k = mid; i < height; i+=2, k++) {
				tmp[k][j] = (temp[i][j]-temp[i-1][j])/2;
			}
		}
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				if (i < height && j < width) {
					x[i][j] = tmp[i][j];
				} else {
					x[i][j] = 0;
				}
			}
		}
		return x;
	}
	//naive decode
	double[][] decode(double[][] x, int width, int height) {
		int mid = height/2;
		double[][] temp = new double[height][width];
		double[][] tmp = new double[height][width];
		//System.out.println(width+" : " + mid + " , " + x.length);
		//do row first
		for (int i = 0; i < height; i++) {
			//L pass filter
			for (int j = 0, k = 0; j < mid; j++, k+=2) {
				temp[i][k] = (x[i][j]+x[i][j+mid]);
			}
			//H pass filter
			for (int j = 0, k = 1; j < mid; j++, k+=2) {
				temp[i][k] = (x[i][j]-x[i][j+mid]);
			}
		}
		//do column
		for (int j = 0; j < width; j++) {
			//L pass filter
			for (int i = 0, k = 0; i < mid; i++, k+=2) {
				tmp[k][j] = (temp[i][j]+temp[i+mid][j]);
			}
			//H pass filter
			for (int i = 0, k = 1; i < mid; i++, k+=2) {
				tmp[k][j] = (temp[i][j]-temp[i+mid][j]);
			}
		}
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				if (i < height && j < width) {
					x[i][j] = tmp[i][j];
				} else {
					x[i][j] = 0;
				}
			}
		}
		return x;
	}
	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int len = width*height;
			int frameLength = len*3;
			Y = new double[len];
			Cb = new double[len];
			Cr = new double[len];
			yy = new double[height][width];
			cb = new double[height][width];
			cr = new double[height][width];
			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			byte[] bytes = new byte[frameLength];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int j = 0; j < width; j++)
				{
					double R = bytes[ind]&0xff;
					double G = bytes[ind+height*width]&0xff;
					double B = bytes[ind+height*width*2]&0xff;
					//double R = r ,G = g, B = b; 
					yy[y][j] = R*matrix[0][0]+ G*matrix[0][1]+ B*matrix[0][2];
					cb[y][j] = R*matrix[1][0]+ G*matrix[1][1]+ B*matrix[1][2];
					cr[y][j] = R*matrix[2][0]+ G*matrix[2][1]+ B*matrix[2][2];

					// ICT.add(Y[i]);
					// ICT.add(Cb[i]);
					// ICT.add(Cr[i]);
					
					// int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					// //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					// img.setRGB(j,y,pix);
					ind++;
					// i++;
				}
			}
			//showIms(0, len, 512);
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
	double[][] FWT97(double[][] data, int levels, int channel)
	{
		int w = data.length;
		int h = data[0].length;
		if (naive) {
			h = w = N;
		}
		for (int i = 0; i < levels; i++)
		{
			if (naive) {
				data = encode(data, w, h);
			} else {
				fwt2d(data, w, h);
				fwt2d(data, w, h);
			}
			w >>= 1;
			h >>= 1;
			double temp [][] = new double[N][N];
			for (int j = 0; j < h; j++) {
				for (int k = 0; k < w; k++) {
					 temp[j][k] = data[j][k];
				}
			}
			if (channel == 0) ictY.add(temp);
			else if (channel == 1) ictCb.add(temp);
			else ictCr.add(temp);
		}

		return data;
	} 

	/// <summary>
	///   Inverse biorthogonal 9/7 2D wavelet transform
	/// </summary>
	/// 
	double[][] IWT97(double[][] data, int levels)
	{
		int w = data.length;
		int h = data[0].length;

		for (int i = 0; i < levels - 1; i++)
		{
			h >>= 1;
			w >>= 1;
		}

		for (int i = 0; i < levels; i++)
		{
			if (naive) {
				//System.out.println("I got " + data.length);
				data = decode(data, w, h);
			} else {
				data = iwt2d(data, w, h);
				data = iwt2d(data, w, h);
			}
			h <<= 1;
			w <<= 1;
		}

		return data;
	}

	public void show2D(int len) {

		//https://en.wikipedia.org/wiki/JPEG_2000
		//https://stackoverflow.com/questions/19621847/java-rgb-color-space-to-ycrcb-color-space-conversion
		//first transfer it to Y Cr Cb
		imgOne = new BufferedImage(len, len, BufferedImage.TYPE_INT_RGB);
		//System.out.println(len+ " : " + start + " : " + Y.length);
		int r, g, b;
		for (int i = 0; i < len; i++) {
			for (int j = 0; j < len; j++) {
				r = (int)(yy[i][j]*inverse[0][0]+cb[i][j]*inverse[0][1]+cr[i][j]*inverse[0][2]);
				g = (int)(yy[i][j]*inverse[1][0]+cb[j][i]*inverse[1][1]+cr[i][j]*inverse[1][2]);
				b = (int)(yy[i][j]*inverse[2][0]+cb[i][j]*inverse[2][1]+cr[i][j]*inverse[2][2]);
				r = Math.min(255, Math.max(0, r));
				g = Math.min(255, Math.max(0, g));
				b = Math.min(255, Math.max(0, b));
				int val = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    			imgOne.setRGB(j,i,val);//it just like scan row by row from col 1 to n
			}
		}
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
	}
	public static void main(String[] args) {
		JPG2000 j2 = new JPG2000();
		// Read a parameter from command line
		//String param1 = args[1];
		//System.out.println("The second parameter was: " + param1);
		int mode = Integer.parseInt(args[1]);
		//Progressive Encoding-Decoding Implementation
		/*This is when n = -1. In this case you will go through the creation of the entire DWT representation till level 0. 
		Then decode each level recursively and display the output. The first display will be at level 0, then level 1 and so on till you reach level 9. 
		You should see the image progressively improving with details.*/
		Boolean presentAll = false;
		if (mode < 0) {
			mode = 0;
			presentAll = true;	
		}
		// Read in the specified image
		j2.imgOne = new BufferedImage(j2.width, j2.height, BufferedImage.TYPE_INT_RGB);
		j2.readImageRGB(j2.width, j2.height, args[0], j2.imgOne);

		//if it is level 9 then just show
		if (mode == 9) {
			j2.show2D(j2.width);
			return;
		}

		if (presentAll) {
			//show original pic first
			//j2.show2D(j2.width);
			j2.FWT97(j2.yy, 9, 0);
			j2.FWT97(j2.cb, 9, 1);
			j2.FWT97(j2.cr, 9, 2);
			for (int i = 0; i < 8; i++) {
				j2.yy = j2.IWT97(j2.ictY.get(8-i), 9-i);
				j2.cb = j2.IWT97(j2.ictCb.get(8-i), 9-i);
				j2.cr = j2.IWT97(j2.ictCr.get(8-i), 9-i);
				j2.show2D(j2.width);
			}
			j2.readImageRGB(j2.width, j2.height, args[0], j2.imgOne);
			j2.show2D(j2.width);
		} else {
			j2.yy = j2.FWT97(j2.yy, 9-mode, 0);
			j2.cb = j2.FWT97(j2.cb, 9-mode, 1);
			j2.cr = j2.FWT97(j2.cr, 9-mode, 2);
			j2.show2D(j2.width);
			//System.out.println("I give you "+ j2.yy.length);
			j2.yy = j2.IWT97(j2.ictY.get(8-mode), 9-mode);
			j2.cb = j2.IWT97(j2.ictCb.get(8-mode), 9-mode);
			j2.cr = j2.IWT97(j2.ictCr.get(8-mode), 9-mode);
			j2.show2D(j2.width);
		}
	}

}