def calculateHiresOffset(int y){calculateTextOffset(y >> 3) + ((y & 7) << 10)}
def calculateTextOffset(int y){((y & 7) << 7) + 40 * (y >> 3)}

def inputFile = "grecco.xml" as File
def records = new XmlParser().parseText(inputFile.getText())

records.image.each{
	def name=it.'@name'
	def data=it.displayData[0].text()

	byte[] output = new byte[0x02000]
	def counter=0;
	for (int y=0; y < 192; y++) {
		int offset = calculateHiresOffset(y);
		for (int x=0; x < 40; x++) {
			String s = data[counter..counter+1]
			counter+=2;
			int num = Integer.parseInt(s, 16);
			output[offset + x] = num;
		}
	}

	File f = "/tmp/"+name+".bin#062000" as File
	f.setBytes(output)
}
