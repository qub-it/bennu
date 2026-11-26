package org.fenixedu.bennu.io.domain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.apache.commons.lang3.StringUtils;

import com.qubit.terra.framework.services.ServiceProvider;
import com.qubit.terra.framework.services.accessControl.AccessControlService;

public final class GroupBasedFile extends GroupBasedFile_Base {
    public GroupBasedFile(String displayName, String filename, byte[] content, Group accessGroup) {
        super();
        init(displayName, filename, content);
        setAccessGroup(accessGroup);
    }

    /**
     * Creates a new {@link GroupBasedFile} with the contents of the given file and the provided name and access group.
     * 
     * @param displayName
     *            The pretty name for this file
     * @param filename
     *            The low-level filename for this file
     * @param file
     *            The file from which the contents of the newly created file are based upon
     * @param accessGroup
     *            The access group for this file
     * @throws IOException
     *             If an error occurs while reading the input file or storing it in the underlying storage
     */
    public GroupBasedFile(String displayName, String filename, File file, Group accessGroup) throws IOException {
        super();
        init(displayName, filename, file);
        setAccessGroup(accessGroup);
    }
    
    
    public GroupBasedFile(String displayName, String filename, InputStream stream, Group accessGroup) throws IOException {
        super();
        init(displayName, filename, stream);
        setAccessGroup(accessGroup);
    }


    public Group getAccessGroup() {
        return getGroup().toGroup();
    }

    public void setAccessGroup(Group accessGroup) {
        setGroup(accessGroup.toPersistentGroup());
    }

    @Override
    protected void setGroup(PersistentGroup group) {
        super.setGroup(group);
        setAccessControlExpression(null);
    }

    @Override
    public boolean isAccessible(User user) {
        return isAccessible(user, getAccessControlExpression(), getGroup());
    }

    static boolean isAccessible(User user, String omnisExpression, PersistentGroup legacyAccessGroup) {
        String expression = StringUtils.defaultIfBlank(omnisExpression, legacyAccessGroup.toGroup().getExpression());
        return ServiceProvider.getService(AccessControlService.class).hasAccess(user.getUsername(), expressionToCheck);
    }

    @Override
    public void delete() {
        setGroup(null);
        setAccessControlExpression(null);
        super.delete();
    }
}
