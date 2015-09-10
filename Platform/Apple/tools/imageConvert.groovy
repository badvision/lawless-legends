/*
 * Copyright (C) 2015 The 8-Bit Bunch. Licensed under the Apache License, Version 1.1 
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-1.1>.
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License.
 */

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
