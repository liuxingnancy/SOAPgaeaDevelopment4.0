package org.bgi.flexlab.gaea.data.structure.positioninformation.depth;

import org.bgi.flexlab.gaea.data.structure.positioninformation.CalculatePositionInforamtionInterface;
import org.bgi.flexlab.gaea.data.structure.positioninformation.IntPositionInformation;
import org.bgi.flexlab.gaea.util.SAMInformationBasic;
import org.bgi.flexlab.gaea.data.structure.positioninformation.BamPositionInformation;

public class PositionDepthGender extends IntPositionInformation implements CalculatePositionInforamtionInterface<SAMInformationBasic>{

	public PositionDepthGender(int windowSize) {
		super(windowSize);
		// TODO Auto-generated constructor stub
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void add(BamPositionInformation posInfo) {
		// TODO Auto-generated method stub
		if(posInfo.getMappingQual() > 20 && !posInfo.isDup())
			info[posInfo.distBetweenRefPosAndWinStart()]++;
	}
}