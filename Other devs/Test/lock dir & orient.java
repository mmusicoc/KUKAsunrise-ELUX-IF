/*
	
	private void configPadKeysCONSTRAIN() {
		IUserKeyListener padKeysListener = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					switch (key.getSlot()) {
						case 0:  						// KEY - CONSTRAIN POSITION
							if (state == States.teach) {
								lockDirection();
							} else padLog("Key not available in this mode.");
							break;
						case 1: 						// KEY - CONSTRAIN ORIENTATION
							if (state == States.teach) {
								lockOrientation();
							} else padLog("Key not available in this mode.");
							break;
						case 2:
							break;
						case 3:
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "CONSTRAIN", "Direction", "Orientation", "Approach", " ");
	}
	
	private void lockDirection() {
		int promptAns = pad.question("Do you want to force any direction?", "X", "Y", "Z", "XY", "XZ", "YZ", "NONE");
		softMode.parametrize(CartDOF.TRANSL).setStiffness(0.1).setDamping(1);
		softMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1);
		switch (promptAns) {
			case 0:
				softMode.parametrize(CartDOF.Y).setStiffness(5000).setDamping(0.5);
				softMode.parametrize(CartDOF.Z).setStiffness(5000).setDamping(0.5);
				break;
			case 1:
				softMode.parametrize(CartDOF.X).setStiffness(5000).setDamping(1);
				softMode.parametrize(CartDOF.Z).setStiffness(5000).setDamping(1);
				break;
			case 2:
				softMode.parametrize(CartDOF.X).setStiffness(5000).setDamping(1);
				softMode.parametrize(CartDOF.Y).setStiffness(5000).setDamping(1);
				break;
			case 3:
				softMode.parametrize(CartDOF.Z).setStiffness(5000).setDamping(1);
			case 4:
				softMode.parametrize(CartDOF.Y).setStiffness(5000).setDamping(1);
			case 5:
				softMode.parametrize(CartDOF.X).setStiffness(5000).setDamping(1);
			case 6: break;
		}
		posHoldMotion.cancel();
		//posHold = new PositionHold(softMode, -1, null);
		posHoldMotion = kiwa.moveAsync(posHold);
	}
	
	private void lockOrientation() {
		Frame currentFrame = kiwa.getCurrentCartesianPosition(gripper.getFrame("/TCP"));
		int promptAns = pad.question("Do you want to force any orientation?", "-Z", "+X", "-X", "+Y", "-Y", "NONE");
		softMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1);
		switch (promptAns) {
			case 0: 
				softMode.parametrize(CartDOF.A).setStiffness(0.1).setDamping(1);
				break;
			case 5: break;
			default: break;
		}
	}
	
	*/