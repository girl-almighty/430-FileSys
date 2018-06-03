Public class SuperBlock 
{
    private final int defaultInodeBlocks = 64;
    public int totalBlocks;
    public int totalInodes;
    public int freeList;

    public SuperBlock(int diskSize)
    {
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        totalInodes = SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock,8);

        if(totalBlocks == diskSize && totalInodes > 0 && freeList >= 2)
            return;
        else
        {
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }
	
	// write private variables to the disk 
    public void sync()
    {
        byte[] block = new byte[512];
        SysLib.int2bytes(totalBlocks, block, 0);
        SysLib.int2bytes(inodeBlocks, block, 4);
        SysLib.int2bytes(freeList, block, 8);
        SysLib.rawwrite(0, block);
    }
	
	// return first freeBlock from freeList
    public getFreeBlock()
	{
        if (freeList != -1) {
            byte[] data = new byte[512];
            SysLib.rawread(freeList, data);
			int freeBlock = freeList;
            freeList = SysLib.bytes2int(data, 0);
			SysLib.int2bytes(0,data,0);
			SysLib.rawwrite(freeBlock,data);
            return freeBlock;
        }

        return -1;
    }

    public boolean returnBlock(int blockNumber)
    {	
		if (blockNumber < 0){
			return false;
		}
        byte[] buf = new byte[Disk.blockSize];
        for (int i = 0; i < 512; i++){
			buf[i] = 0;
		}
		SysLib.int2bytes(freeList, buf, 0);
		SysLib.rawwrite(blockNumber, buf);
		freeList = blockNumber;
		return true;
    }

    public int format(int files)
    {
        totalInodes = files;

        for(short i = 0; i < totalInodes; i++)
        {
            Inode newInode = new Inode();
            newInode.toDisk(i);
        }

        if(totalInodes % 16 == 0)
            int offSet = 1;
        else
            int offSet = 2;
        freeList = numInodes / 16 + offSet;

        byte[] block;
        for(int i = freeList; i < totalBlocks - 1; i++)
        {
            block = new byte[Disk.blockSize];
            for(int j = 0; j < Disk.blockSize; j++)
                block[j] = (byte) 0;
            SysLib.int2bytes(i+1, block, 0);
            SysLib.rawwrite(totalBlocks - 1, block);        
        }
        SysLib.int2bytes(-1, block, 0);
        SysLib.rawwrite(totalBlocks - 1, block);
        sync();
    }
}