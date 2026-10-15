package org.fenixedu.bennu.portal.domain;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.qubit.terra.portal.domain.menus.MenuUrlParameter;

public class MenuItemUrlParameter extends MenuItemUrlParameter_Base implements MenuUrlParameter {

    private MenuItemUrlParameter() {
        super();
    }

    public MenuItemUrlParameter(String name, boolean required, MenuFunctionality item) {
        this();
        setMenuFunctionality(item);
        setName(name);
        setRequired(required);
    }

    @Override
    public void setName(final String name) {
        if (StringUtils.isBlank(name)) {
            throw BennuPortalDomainException.menuUrlParameterWithNoName();
        }
        Optional.ofNullable(getMenuFunctionality()).ifPresent(entry -> {
            if (getMenuFunctionality().getUrlParameters().stream().filter(p -> !p.equals(this))
                    .anyMatch(p -> name.equals(p.getName()))) {
                throw BennuPortalDomainException.menuUrlParameterWithNameAlreadyExists(name);
            }
        });
        super.setName(name);
    }

    @Override
    public boolean isRequired() {
        return super.getRequired();
    }

    @Override
    public void delete() {
        setMenuFunctionality(null);
        deleteDomainObject();
    }

}
