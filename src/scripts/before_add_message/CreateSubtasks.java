package scripts.before_add_message;

import com.trackstudio.app.TriggerManager;
import com.trackstudio.app.adapter.AdapterManager;
import com.trackstudio.app.session.SessionContext;
import com.trackstudio.exception.GranException;
import com.trackstudio.external.OperationTrigger;
import com.trackstudio.secured.*;
import com.trackstudio.tools.textfilter.AccentedFilter;
import java.util.List;

public class CreateSubtasks implements OperationTrigger {

    private static final String TODO = "ff80818131dbce630131dbe092910012";
    private static final String WORKER_UDF = "ff808181325dcb6d01325dd4f7b6003a";
    private static final String WORKER_ROLE = "ff80818131cd4e930131cd9781a501e5";

    protected void appendACL(SecuredTaskBean task, String targetUser, String role) throws GranException{
        SessionContext sc = task.getSecure();
        if (targetUser!=null){


        List<SecuredTaskAclBean> sourceAcl = AdapterManager.getInstance().getSecuredAclAdapterManager().getTaskAclList(sc,
                task.getId());

        boolean exists = false;
        for (SecuredTaskAclBean tb: sourceAcl){
            if (tb.canManage() && tb.getTaskId().equals(task.getId())){
                SecuredPrstatusBean group = tb.getGroup();
                SecuredUserBean user = tb.getUser();
                SecuredPrstatusBean prstatus = tb.getPrstatus();

            exists = targetUser.equals(user.getId()) && role.equals(prstatus.getId());
            if (exists) break;
            }
        }
                if (!exists){
                    // create
                    String aclid =
AdapterManager.getInstance().getSecuredAclAdapterManager().createAcl(sc, task.getId(), null,
                           targetUser,  null);
AdapterManager.getInstance().getSecuredAclAdapterManager().updateTaskAcl(sc, aclid, role, false);

                }
            }

    }

    
    public SecuredMessageTriggerBean execute(SecuredMessageTriggerBean message) throws GranException {
        String description = message.getDescription();
        SecuredUDFBean udf = AdapterManager.getInstance().getSecuredFindAdapterManager().findUDFById(message.getSecure(), WORKER_UDF);
        String worker = message.getUdfValue(udf.getCaption());
        String workerId=null;
        if (worker!=null && worker.length()>0){
            SecuredUserBean u = AdapterManager.getInstance().getSecuredUserAdapterManager().findByName(message.getSecure(), worker);
            if (u!=null) {
                workerId = u.getId();
                appendACL(message.getTask(), workerId, WORKER_ROLE);
            }
        }
        StringBuffer newDescription = new StringBuffer();
        if (description.contains("*")) {
            String[] lines = description.split("<br />");
            for (String line : lines) {
                if (line.trim().startsWith("*")) {
                    String taskName = line.trim().substring(1);
                    if (taskName.length() > 0) {
                        StringBuffer b = new StringBuffer(taskName);
                        b = AccentedFilter.unescape(b);
                        String startState = "ff80818131d296560131d2974e4a0003";
                        if (message.getTask().getStatusId().equals("ff80818131cd4e930131cd506aa5000b"))
                            startState = "ff80818131e179ef0131e1e9306400e0";
                        String taskId = TriggerManager.getInstance().createTask(message.getSecure(), TODO, null,
                                b.toString().trim(), "", null, null, null, message.getTaskId(),
                                workerId, null, true, null, startState, null);
                        if (taskId != null) {
                            SecuredTaskBean t = new SecuredTaskBean(taskId, message.getSecure());
                            newDescription.append("##").append(t.getNumber()).append("<br/>");
                        }
                    }

                } else newDescription.append(line).append("<br />");
            }
            message.setDescription(newDescription.toString());
        }
        return message;
    }
}
