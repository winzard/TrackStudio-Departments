package scripts.after_create_task;

import scripts.CommonDepartments;

import com.trackstudio.exception.GranException;
import com.trackstudio.external.TaskTrigger;
import com.trackstudio.secured.SecuredTaskTriggerBean;

public class RegisterService extends CommonDepartments implements TaskTrigger {

	@Override
	public SecuredTaskTriggerBean execute(SecuredTaskTriggerBean task)
			throws GranException {
		// TODO Auto-generated method stub
		return null;
	}

}
