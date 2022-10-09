sample command
>> java -ea ImageDisplay.java img/miamibeach.rgb 1 1 1 1 1 0 
>> java -ea ImageDisplay.java img/miamibeach.rgb 1 2 2 1 1 0 
>> java -ea ImageDisplay.java img/miamibeach.rgb 1 1 1 0.5 0.5 0 
>> java -ea ImageDisplay.java img/miamibeach.rgb 1 1 1 0.5 0.5 1
>> java -ea ImageDisplay.java img/lake-forest_1920w_1080h.rgb 20 1 1 0.5 0.5 1

Please  use "-ea" option to assert the input is valid.
The output image will be saved in img/ as jpeg file for future check.
Please use "ctrl+c" to teminate the code before run next command.

parameter
String imgPath = args[0];
int subSamplingY = Integer.parseInt(args[1]);
int subSamplingU = Integer.parseInt(args[2]);
int subSamplingV = Integer.parseInt(args[3]);
float Sw = Float.parseFloat(args[4]);
float Sh = Float.parseFloat(args[5]);
int A =  Integer.parseInt(args[6]);