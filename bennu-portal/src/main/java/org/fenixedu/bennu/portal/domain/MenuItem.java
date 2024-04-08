package org.fenixedu.bennu.portal.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.Atomic;

/**
 * Base class for items that are presented in an application's menu.
 * 
 * {@code MenuItem}s are either a {@link MenuContainer}, aggregating other items or a {@link MenuFunctionality}, representing a
 * concrete entry in the menu.
 * 
 * Note that a {@link MenuItem} is immutable, meaning that once it is installed in the menu, it is not possible to modify any of
 * its properties. If you desire to do so, {@code delete} the item and create a new one with the new values.
 * 
 * {@code MenuItem}s are {@link Comparable} according to their order in the menu.
 * 
 * @see MenuContainer
 * @see MenuFunctionality
 * 
 */
public abstract class MenuItem extends MenuItem_Base implements com.qubit.terra.portal.domain.menus.MenuItem {

    protected MenuItem() {
        super();
        setOrd(1);
    }

    protected final void init(MenuContainer parent, boolean visible, String accessGroup, LocalizedString title,
            LocalizedString description, String path) {
        setVisible(visible);
        setAccessGroup(Group.parse(accessGroup));
        setDescription(description);
        setTitle(title);
        setPath(path);
        if (parent != null) {
            parent.addChild(this);
        }
        setFullPath(computeFullPath());
    }

    protected final void init(MenuContainer parent, MenuItem original) {
        setVisible(original.getVisible());
        setAccessGroup(original.getAccessGroup());
        setDescription(original.getDescription());
        setTitle(original.getTitle());
        setPath(original.getPath());
        setLayout(original.getLayout());
        if (parent != null) {
            parent.addChild(this);
        }
        setFullPath(computeFullPath());
    }

    public Group getAccessGroup() {
        return getGroup().toGroup();
    }

    public void setAccessGroup(Group group) {
        setGroup(group.toPersistentGroup());
    }

    @Override
    protected void setGroup(PersistentGroup group) {
        super.setGroup(group);
        if (getParent() != null) {
            getParent().updateAccessGroup();
        }
    }

    @Override
    public boolean isMenuContainer() {
        return this instanceof MenuContainer;
    }

    @Override
    public boolean isMenuFunctionality() {
        return this instanceof MenuFunctionality;
    }

    /**
     * Deletes this item, removing it from the menu.
     */
    @Atomic
    @Override
    public void delete() {
        setParent(null);
        setGroup(null);
        deleteDomainObject();
    }

    /**
     * Determines whether this {@link MenuItem} and all its parents are available for the given {@link User}.
     * 
     * @param user
     *            The user to verify
     * @return
     *         Whether the given user can access this item
     */
    public boolean isAvailable(User user) {
        return getGroup().isMember(user) && getParent().isAvailable(user);
    }

    /*
     * Returns whether this item is available for the current user.
     * Implementation Node: This method ONLY checks the current node, not the full chain!
     */
    protected boolean isItemAvailableForCurrentUser() {
        return getGroup().isMember(Authenticate.getUser());
    }

    /**
     * Returns whether the Item should be visible when rendering a menu.
     * 
     * @return
     *         {@code true} if this item is visible
     */
    @Override
    public boolean isItemVisible() {
        return isVisible();
    }

    @Deprecated
    public boolean isVisible() {
        return getVisible();
    }

    @Deprecated
    public void setVisible(boolean visible) {
        super.setVisible(visible);
    }

    @Override
    public void setItemVisible(boolean visible) {
        setVisible(visible);
    }

    @Override
    public MenuContainer getParent() {
        //FIXME: remove when the framework enables read-only slots
        return super.getParent();
    }

    @Override
    public String getPath() {
        //FIXME: remove when the framework enables read-only slots
        return super.getPath();
    }

    @Override
    public String getFullPath() {
        //FIXME: remove when the framework enables read-only slots
        return super.getFullPath();
    }

    public MenuContainer getAsMenuContainer() {
        if (isMenuContainer()) {
            return (MenuContainer) this;
        }
        throw new IllegalStateException("Not a MenuContainer");
    }

    public MenuFunctionality getAsMenuFunctionality() {
        if (isMenuFunctionality()) {
            return (MenuFunctionality) this;
        }
        throw new IllegalStateException("Not a MenuFunctionality");
    }

    public List<MenuItem> getPathFromRoot() {
        List<MenuItem> result = new ArrayList<MenuItem>();
        MenuItem current = this;
        while (current.getParent() != null) {
            result.add(0, current);
            current = current.getParent();
        }
        return result;
    }

    public abstract MenuItem moveTo(MenuContainer container);

    @Override
    public boolean isItemRestricted() {
        if (getParent() == null) {
            return true;
        }
        return getRestricted() != null ? getRestricted() : getParent().isItemRestricted();
    }

    public String getRecursiveProviderImplementation() {
        if (!StringUtils.isEmpty(getProviderImplementation())) {
            return getProviderImplementation();
        }
        return getParent() != null ? getParent().getRecursiveProviderImplementation() : null;
    }

    @Override
    public Integer getPosition() {
        return super.getOrd();
    }

    @Override
    public void setItemName(com.qubit.terra.framework.tools.primitives.LocalizedString name) {
        setTitle(BundleUtil.convertToBennuLocalizedString(name));
    }

    @Override
    public void setPosition(Integer position) {
        super.setOrd(position);
    }

    @Override
    public com.qubit.terra.framework.tools.primitives.LocalizedString getItemName() {
        return BundleUtil.convertToPlatformLocalizedString(super.getTitle());
    }

    @Override
    public String getItemPath() {
        return getPath();
    }

    @Override
    public void setItemPath(String path) {
        setPath(path);
    }

    @Override
    public String getItemIcon() {
        return super.getIcon();
    }

    @Override
    public void setItemIcon(String icon) {
        setIcon(icon);
    }

    @Override
    public String getMenuLayout() {
        return super.getLayout();
    }

    @Override
    public void setMenuLayout(String layout) {
        setLayout(layout);
    }

    @Override
    public void setMenuItemFullPath(String path) {
        setFullPath(path);
    }

    @Override
    public com.qubit.terra.framework.tools.primitives.LocalizedString getItemDescription() {
        return BundleUtil.convertToPlatformLocalizedString(super.getDescription());
    }

    /**
     * Determines whether this {@link MenuItem} and all its parents are available for the currently logged user.
     * This method is a shorthand for <code>isAvailable(Authenticate.getUser())</code>.
     * 
     * @return
     *         Whether the currently logged user can access this item
     */
    @Override
    public boolean isAvailableForCurrentUser() {
        return isAvailable(Authenticate.getUser());
    }

    @Override
    public com.qubit.terra.portal.domain.menus.MenuContainer getParentContainer() {
        return super.getParent();
    }

    @Override
    public String getItemProviderImplementation() {
        return getProviderImplementation();
    }

    @Override
    public void setItemProviderImplementation(String providerImplementation) {
        this.setProviderImplementation(providerImplementation);
    }

    @Override
    public MenuContainer asMenuContainer() {
        return getAsMenuContainer();
    }

    @Override
    public MenuFunctionality asMenuFunctionality() {
        return getAsMenuFunctionality();
    }

    @Override
    public String getAccessControlExpression() {
        return this.getAccessGroup() == null ? null : this.getAccessGroup().getExpression();
    }

    @Override
    public void setAccessControlExpression(String expression) {
        setAccessGroup(Group.parse(expression));
    }

    @Override
    public void setItemDescription(com.qubit.terra.framework.tools.primitives.LocalizedString description) {
        this.setDescription(BundleUtil.convertToBennuLocalizedString(description));
    }

    @Override
    public void setItemRestricted(Boolean restricted) {
        setRestricted(restricted);
    }

}
