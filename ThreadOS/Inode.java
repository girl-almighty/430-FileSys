public class Inode {
   private final static int iNodeSize = 32;      // fix to 32 bytes
   private final static int directSize = 11;     // # direct pointers

   public int length;                      				// file size in bytes
   public short count;                     				// # file-table entries pointing to this
   public short flag;                      				// 0 = unused, 1 = used, ...
   public short direct[] = new short[directSize]; // direct pointers
   public short indirect;                   			// a indirect pointer

   Inode( ) {                           			 		// a default constructor
     length = 0;
     count = 0;
     flag = 1;
     for ( int i = 0; i < directSize; i++ )
       direct[i] = -1;
     indirect = -1;
   }
		
	 // find iNumber inode from disk and save in memeory 
   Inode( short iNumber ) {                  			// retrieving inode from disk
     // design it by yourself.
     int blockNum = 1 + iNumber / 16;							// find the disk block with inode number
     byte data[] = new byte[512]									// setting the buffer size of a block 512 bytes

			SysLib.rawread(blockNum, data);							// read from inode block into data buffer
			
			int offset = (iNumber % 16) * iNodeSize;		// locate where we are
			
			// locates corresponding inode information in the corresponding block
			length = SysLib.bytes2int(data, offset);
			offset += 4;																// increase offset 4 bytes for integer "length"
			count = SysLib.bytes2short(data, offset);
			offset += 2;																// increase offset 2 bytes for short 
			flag = SysLib.bytes2short(data, offset);
			offset += 2;																// increase offset 2 bytes for short
			for(int i =0; i < directSize; i++) {				// direct pointers
				direct[i] = SysLib.bytes2short(data, offset);
				offset += 2;
			}
			indirect = SysLib.bytes2short(data, offset);// indirect pointers
	 }

	 // write the iNumber inode in to disk 
   int toDisk( short iNumber ) {              
     // design it by yourself.
     
     byte[] data = new byte[512];
     
     int blockNum = 1 + iNumber / 16;							// find the block where the inode is
     SysLib.rawread(block,data);									// read origin block
     
     int offset = (iNumber % 16) * 32;						// find offset in the block 
			SysLib.int2bytes(length, data, offset);			// write to buffer
			offset += 4;
			SysLib.short2bytes(count, data, offset);
			offset += 2;
			SysLib.short2bytes(flag, data, offset);
			offset += 2;
			for(int i = 0; i < directSize; i++) {
				SysLib.short2bytes(this.direct[i], data, offset);
				offset += 2;
			}
			SysLib.short2bytes(this.indirect, data, offset);
			offset += 2;
			
     SysLib.rawwrite(blockNum, data);							// write to disk
   }

   // return the indirect pointer
   int findIndexBlock(){
   	return indirect;
   }

   // set indirect block with -1
   boolean setIndexBlock(short indexBlockNumber){
   		//if any direct block unused, return false
		for(int i = 0; i < 11; i++){
			if(direct[i] == -1){
				return false;
			}
		}
		// if the indirect block been used, return false
	   if (indirect != -1){
			return false;
	   }
	   else{
			indirect = indexBlockNumber;
			byte data = new byte[512];

		    // write 265 indirect pointers with -1
			for (int i = 0; i < 256; i++){
				SysLib.short2bytes((short)-1, data, i*2);
			}

			SysLib.rawwrite(indexBlockNumber, data);
			return true;
	   }
   }

   // find the target block by given offset
   short findTargetBlock(int offset){
   	// done
   }

   //
   int setTargetBlock(int offset, short block){
   		if (offset / 512 < 11){
   			direct[offset/512] = block;
   			return 0;
		}

   }

   short getIndexBlockNumber(){

   }
   
}
