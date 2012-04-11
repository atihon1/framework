/*
@VaadinApache2LicenseForJavaFiles@
 */
package com.vaadin.ui;

import java.util.Iterator;
import java.util.LinkedList;

import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.event.LayoutEvents.LayoutClickNotifier;
import com.vaadin.terminal.gwt.client.Connector;
import com.vaadin.terminal.gwt.client.MouseEventDetails;
import com.vaadin.terminal.gwt.client.ui.csslayout.CssLayoutConnector.CssLayoutServerRPC;
import com.vaadin.terminal.gwt.client.ui.csslayout.CssLayoutConnector.CssLayoutState;
import com.vaadin.terminal.gwt.client.ui.LayoutClickEventHandler;

/**
 * CssLayout is a layout component that can be used in browser environment only.
 * It simply renders components and their captions into a same div element.
 * Component layout can then be adjusted with css.
 * <p>
 * In comparison to {@link HorizontalLayout} and {@link VerticalLayout}
 * <ul>
 * <li>rather similar server side api
 * <li>no spacing, alignment or expand ratios
 * <li>much simpler DOM that can be styled by skilled web developer
 * <li>no abstraction of browser differences (developer must ensure that the
 * result works properly on each browser)
 * <li>different kind of handling for relative sizes (that are set from server
 * side) (*)
 * <li>noticeably faster rendering time in some situations as we rely more on
 * the browser's rendering engine.
 * </ul>
 * <p>
 * With {@link CustomLayout} one can often achieve similar results (good looking
 * layouts with web technologies), but with CustomLayout developer needs to work
 * with fixed templates.
 * <p>
 * By extending CssLayout one can also inject some css rules straight to child
 * components using {@link #getCss(Component)}.
 * 
 * <p>
 * (*) Relative sizes (set from server side) are treated bit differently than in
 * other layouts in Vaadin. In cssLayout the size is calculated relatively to
 * CSS layouts content area which is pretty much as in html and css. In other
 * layouts the size of component is calculated relatively to the "slot" given by
 * layout.
 * <p>
 * Also note that client side framework in Vaadin modifies inline style
 * properties width and height. This happens on each update to component. If one
 * wants to set component sizes with CSS, component must have undefined size on
 * server side (which is not the default for all components) and the size must
 * be defined with class styles - not by directly injecting width and height.
 * 
 * @since 6.1 brought in from "FastLayouts" incubator project
 * 
 */
public class CssLayout extends AbstractLayout implements LayoutClickNotifier {

    private CssLayoutServerRPC rpc = new CssLayoutServerRPC() {

        public void layoutClick(MouseEventDetails mouseDetails,
                Connector clickedConnector) {
            fireEvent(LayoutClickEvent.createEvent(CssLayout.this,
                    mouseDetails, clickedConnector));
        }
    };
    /**
     * Custom layout slots containing the components.
     */
    protected LinkedList<Component> components = new LinkedList<Component>();

    public CssLayout() {
        registerRpc(rpc);
    }

    /**
     * Add a component into this container. The component is added to the right
     * or under the previous component.
     * 
     * @param c
     *            the component to be added.
     */
    @Override
    public void addComponent(Component c) {
        // Add to components before calling super.addComponent
        // so that it is available to AttachListeners
        components.add(c);
        try {
            super.addComponent(c);
            requestRepaint();
        } catch (IllegalArgumentException e) {
            components.remove(c);
            throw e;
        }
    }

    /**
     * Adds a component into this container. The component is added to the left
     * or on top of the other components.
     * 
     * @param c
     *            the component to be added.
     */
    public void addComponentAsFirst(Component c) {
        // If c is already in this, we must remove it before proceeding
        // see ticket #7668
        if (c.getParent() == this) {
            removeComponent(c);
        }
        components.addFirst(c);
        try {
            super.addComponent(c);
            requestRepaint();
        } catch (IllegalArgumentException e) {
            components.remove(c);
            throw e;
        }
    }

    /**
     * Adds a component into indexed position in this container.
     * 
     * @param c
     *            the component to be added.
     * @param index
     *            the index of the component position. The components currently
     *            in and after the position are shifted forwards.
     */
    public void addComponent(Component c, int index) {
        // If c is already in this, we must remove it before proceeding
        // see ticket #7668
        if (c.getParent() == this) {
            // When c is removed, all components after it are shifted down
            if (index > getComponentIndex(c)) {
                index--;
            }
            removeComponent(c);
        }
        components.add(index, c);
        try {
            super.addComponent(c);
            requestRepaint();
        } catch (IllegalArgumentException e) {
            components.remove(c);
            throw e;
        }
    }

    /**
     * Removes the component from this container.
     * 
     * @param c
     *            the component to be removed.
     */
    @Override
    public void removeComponent(Component c) {
        components.remove(c);
        super.removeComponent(c);
        requestRepaint();
    }

    /**
     * Gets the component container iterator for going trough all the components
     * in the container.
     * 
     * @return the Iterator of the components inside the container.
     */
    public Iterator<Component> getComponentIterator() {
        return components.iterator();
    }

    /**
     * Gets the number of contained components. Consistent with the iterator
     * returned by {@link #getComponentIterator()}.
     * 
     * @return the number of contained components
     */
    public int getComponentCount() {
        return components.size();
    }

    @Override
    public void updateState() {
        super.updateState();
        getState().getChildCss().clear();
        for (Component child : this) {
            String componentCssString = getCss(child);
            if (componentCssString != null) {
                getState().getChildCss().put(child, componentCssString);
            }

        }
    }

    @Override
    public CssLayoutState getState() {
        return (CssLayoutState) super.getState();
    }

    /**
     * Returns styles to be applied to given component. Override this method to
     * inject custom style rules to components.
     * 
     * <p>
     * Note that styles are injected over previous styles before actual child
     * rendering. Previous styles are not cleared, but overridden.
     * 
     * <p>
     * Note that one most often achieves better code style, by separating
     * styling to theme (with custom theme and {@link #addStyleName(String)}.
     * With own custom styles it is also very easy to break browser
     * compatibility.
     * 
     * @param c
     *            the component
     * @return css rules to be applied to component
     */
    protected String getCss(Component c) {
        return null;
    }

    /* Documented in superclass */
    public void replaceComponent(Component oldComponent, Component newComponent) {

        // Gets the locations
        int oldLocation = -1;
        int newLocation = -1;
        int location = 0;
        for (final Iterator<Component> i = components.iterator(); i.hasNext();) {
            final Component component = i.next();

            if (component == oldComponent) {
                oldLocation = location;
            }
            if (component == newComponent) {
                newLocation = location;
            }

            location++;
        }

        if (oldLocation == -1) {
            addComponent(newComponent);
        } else if (newLocation == -1) {
            removeComponent(oldComponent);
            addComponent(newComponent, oldLocation);
        } else {
            if (oldLocation > newLocation) {
                components.remove(oldComponent);
                components.add(newLocation, oldComponent);
                components.remove(newComponent);
                components.add(oldLocation, newComponent);
            } else {
                components.remove(newComponent);
                components.add(oldLocation, newComponent);
                components.remove(oldComponent);
                components.add(newLocation, oldComponent);
            }

            requestRepaint();
        }
    }

    public void addListener(LayoutClickListener listener) {
        addListener(LayoutClickEventHandler.LAYOUT_CLICK_EVENT_IDENTIFIER,
                LayoutClickEvent.class, listener,
                LayoutClickListener.clickMethod);
    }

    public void removeListener(LayoutClickListener listener) {
        removeListener(LayoutClickEventHandler.LAYOUT_CLICK_EVENT_IDENTIFIER,
                LayoutClickEvent.class, listener);
    }

    /**
     * Returns the index of the given component.
     * 
     * @param component
     *            The component to look up.
     * @return The index of the component or -1 if the component is not a child.
     */
    public int getComponentIndex(Component component) {
        return components.indexOf(component);
    }

    /**
     * Returns the component at the given position.
     * 
     * @param index
     *            The position of the component.
     * @return The component at the given index.
     * @throws IndexOutOfBoundsException
     *             If the index is out of range.
     */
    public Component getComponent(int index) throws IndexOutOfBoundsException {
        return components.get(index);
    }

}
