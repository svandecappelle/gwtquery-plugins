/*
 * Copyright 2010 The gwtquery plugins team.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package gwtquery.plugins.droppable.client.gwt;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.cellview.client.CellTree.BasicResources;
import com.google.gwt.user.cellview.client.CellTree.Resources;
import com.google.gwt.user.cellview.client.CellTree.Style;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasAnimation;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.TreeViewModel;

import gwtquery.plugins.draggable.client.events.BeforeDragStartEvent;
import gwtquery.plugins.draggable.client.events.DragEvent;
import gwtquery.plugins.draggable.client.events.DragStartEvent;
import gwtquery.plugins.draggable.client.events.DragStopEvent;
import gwtquery.plugins.draggable.client.events.BeforeDragStartEvent.BeforeDragStartEventHandler;
import gwtquery.plugins.draggable.client.events.DragEvent.DragEventHandler;
import gwtquery.plugins.draggable.client.events.DragStartEvent.DragStartEventHandler;
import gwtquery.plugins.draggable.client.events.DragStopEvent.DragStopEventHandler;
import gwtquery.plugins.droppable.client.events.ActivateDroppableEvent;
import gwtquery.plugins.droppable.client.events.DeactivateDroppableEvent;
import gwtquery.plugins.droppable.client.events.DropEvent;
import gwtquery.plugins.droppable.client.events.OutDroppableEvent;
import gwtquery.plugins.droppable.client.events.OverDroppableEvent;
import gwtquery.plugins.droppable.client.events.ActivateDroppableEvent.ActivateDroppableEventHandler;
import gwtquery.plugins.droppable.client.events.DeactivateDroppableEvent.DeactivateDroppableEventHandler;
import gwtquery.plugins.droppable.client.events.DropEvent.DropEventHandler;
import gwtquery.plugins.droppable.client.events.OutDroppableEvent.OutDroppableEventHandler;
import gwtquery.plugins.droppable.client.events.OverDroppableEvent.OverDroppableEventHandler;
import gwtquery.plugins.droppable.client.gwt.extend.com.google.gwt.user.cellview.client.AbstractCellTree;
import gwtquery.plugins.droppable.client.gwt.extend.com.google.gwt.user.cellview.client.CellBasedWidgetImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the {@link CellTree} allowing dragging and dropping of the tree node by using  {@link DragAndDropNodeInfo}
 * 
 * @author Julien Dramaix (julien.dramaix@gmail.com)
 *
 */
@SuppressWarnings("deprecation")
public class DragAndDropCellTree extends AbstractCellTree implements HasAnimation,
    Focusable {


  /**
   * A node animation.
   */
  public abstract static class NodeAnimation extends Animation {

    /**
     * The default animation delay in milliseconds.
     */
    private static final int DEFAULT_ANIMATION_DURATION = 450;

    /**
     * The duration of the animation in milliseconds.
     */
    private int duration = DEFAULT_ANIMATION_DURATION;

    NodeAnimation() {
    }

    /**
     * Return the duration of the animation in milliseconds.
     *
     * @see #setDuration(int)
     */
    public int getDuration() {
      return duration;
    }

    /**
     * Set the duration of the animation in milliseconds.
     * 
     * @param duration the duration in milliseconds
     * @see #getDuration()
     */
    public void setDuration(int duration) {
      this.duration = duration;
    }

    /**
     * Animate a tree node into its new state.
     *
     * @param node the node to animate
     * @param isAnimationEnabled true to animate
     */
    abstract void animate(DragAndDropCellTreeNodeView<?> node, boolean isAnimationEnabled);
  }

  /**
   * A {@link NodeAnimation} that reveals the contents of child nodes.
   */
  public static class RevealAnimation extends NodeAnimation {

    /**
     * Create a new {@link RevealAnimation}.
     *
     * @return the new animation
     */
    public static RevealAnimation create() {
      return new RevealAnimation();
    }

    /**
     * The container that holds the content, includind the children.
     */
    Element contentContainer;

    /**
     * The target height when opening, the start height when closing.
     */
    int height;

    /**
     * True if the node is opening, false if closing.
     */
    boolean opening;

    /**
     * The container that holds the child container.
     */
    private Element animFrame;

    /**
     * The container that holds the children.
     */
    private Element childContainer;

    /**
     * Not instantiable.
     */
    private RevealAnimation() {
    }

    @Override
    protected void onComplete() {
      cleanup();
    }

    @Override
    protected void onStart() {
      if (opening) {
        animFrame.getStyle().setHeight(1.0, Unit.PX);
        animFrame.getStyle().setPosition(Position.RELATIVE);
        animFrame.getStyle().clearDisplay();
        height = contentContainer.getScrollHeight();
      } else {
        height = contentContainer.getOffsetHeight();
      }
    }

    @Override
    protected void onUpdate(double progress) {
      if (opening) {
        double curHeight = progress * height;
        animFrame.getStyle().setHeight(curHeight, Unit.PX);
      } else {
        double curHeight = (1.0 - progress) * height;
        animFrame.getStyle().setHeight(curHeight, Unit.PX);
      }

      // Remind IE6 that we want the overflow to be hidden.
      animFrame.getStyle().setOverflow(Overflow.HIDDEN);
      animFrame.getStyle().setPosition(Position.RELATIVE);
    }

    /**
     * Animate a {@link DragAndDropCellTreeNodeView} into its new state.
     *
     * @param node the {@link DragAndDropCellTreeNodeView} to animate
     * @param isAnimationEnabled true to animate
     */
    @Override
    void animate(DragAndDropCellTreeNodeView<?> node, boolean isAnimationEnabled) {
      // Cancel any pending animations.
      cancel();

      // Initialize the fields.
      this.opening = node.isOpen();
      animFrame = node.ensureAnimationFrame();
      contentContainer = node.ensureContentContainer();
      childContainer = node.ensureChildContainer();

      if (isAnimationEnabled) {
        // Animated.
        int duration = getDuration();
        int childCount = childContainer.getChildCount();
        if (childCount < 4) {
          // Reduce the duration if there are less than four items or it will
          // look really slow.
          duration = (int) ((childCount / 4.0) * duration);
        }
        run(duration);
      } else {
        // Non animated.
        cleanup();
      }
    }

    /**
     * Put the node back into a clean state and clear fields.
     */
    private void cleanup() {
      if (opening) {
        animFrame.getStyle().clearDisplay();
      } else {
        animFrame.getStyle().setDisplay(Display.NONE);
        childContainer.setInnerHTML("");
      }
      animFrame.getStyle().clearHeight();
      animFrame.getStyle().clearPosition();
      this.contentContainer = null;
      this.childContainer = null;
      this.animFrame = null;
    }
  }

  /**
   * A {@link NodeAnimation} that slides children into view.
   */
  public static class SlideAnimation extends RevealAnimation {
    /**
     * Create a new {@link RevealAnimation}.
     *
     * @return the new animation
     */
    public static SlideAnimation create() {
      return new SlideAnimation();
    }

    /**
     * Not instantiable.
     */
    private SlideAnimation() {
    }

    @Override
    protected void onComplete() {
      contentContainer.getStyle().clearPosition();
      contentContainer.getStyle().clearTop();
      contentContainer.getStyle().clearWidth();
      super.onComplete();
    }

    @Override
    protected void onStart() {
      super.onStart();
      if (opening) {
        contentContainer.getStyle().setTop(-height, Unit.PX);
      } else {
        contentContainer.getStyle().setTop(0, Unit.PX);
      }
      contentContainer.getStyle().setPosition(Position.RELATIVE);
    }

    @Override
    protected void onUpdate(double progress) {
      super.onUpdate(progress);
      if (opening) {
        double curTop = (1.0 - progress) * -height;
        contentContainer.getStyle().setTop(curTop, Unit.PX);
      } else {
        double curTop = progress * -height;
        contentContainer.getStyle().setTop(curTop, Unit.PX);
      }
    }
  }

  /**
   * Styles used by {@link BasicResources}.
   */
  @ImportedWithPrefix("gwt-CellTree")
  interface BasicStyle extends Style {
    /**
     * The path to the default CSS styles used by this resource.
     */
    String DEFAULT_CSS = "com/google/gwt/user/cellview/client/CellTreeBasic.css";
  }

  interface Template extends SafeHtmlTemplates {
    @Template("<div class=\"{0}\" style=\"position:absolute;{1}:0px;"
        + "width:{2}px;height:{3}px;\">{4}</div>")
    SafeHtml imageWrapper(String classes, String direction, int width,
        int height, SafeHtml image);

    @Template("<div class=\"{0}\" style=\"position:absolute;{1}:-{2}px;"
        + "width:{2}px;height:{3}px;\">{4}</div>")
    SafeHtml imageWrapperIE6(String classes, String direction, int width,
        int height, SafeHtml image);
  }

  /**
   * Implementation of {@link CellTree}.
   */
  private static class Impl {
    /**
     * Create an image wrapper.
     */
    public SafeHtml imageWrapper(String classes, String direction, int width,
        int height, SafeHtml image) {
      return template.imageWrapper(classes, direction, width, height, image);
    }
  }

  /**
   * Implementation of {@link CellTable} used by IE6.
   */
  @SuppressWarnings("unused")
  private static class ImplIE6 extends Impl {
    @Override
    public SafeHtml imageWrapper(String classes, String direction, int width,
        int height, SafeHtml image) {
      /*
       * In IE6, left/right positions are relative to the inside of the padding
       * instead of the outside of the padding. The bug does not happen on IE7,
       * which maps to the IE6 user agent, so we need a runtime check for IE6.
       */
      if (isIe6()) {
        return template.imageWrapperIE6(classes, direction, width, height, image);
      }
      return super.imageWrapper(classes, direction, width, height, image);
    }

    private native boolean isIe6() /*-{
      return @com.google.gwt.dom.client.DOMImplIE6::isIE6()();
    }-*/;
  }

  /**
   * The default number of children to show under a tree node.
   */
  private static final int DEFAULT_LIST_SIZE = 25;

  private static Resources DEFAULT_RESOURCES;

  private static Impl TREE_IMPL;
  private static Template template;

  private static Resources getDefaultResources() {
    if (DEFAULT_RESOURCES == null) {
      DEFAULT_RESOURCES = GWT.create(Resources.class);
    }
    return DEFAULT_RESOURCES;
  }

  /**
   * A boolean indicating whether or not a cell is being edited.
   */
  boolean cellIsEditing;

  /**
   * A boolean indicating that the widget has focus.
   */
  boolean isFocused;

  /**
   * Set to true while the elements are being refreshed. Events are ignored
   * during this time.
   */
  boolean isRefreshing;

  /**
   * The hidden root node in the tree. Visible for testing.
   */
  final DragAndDropCellTreeNodeView<?> rootNode;

  private char accessKey = 0;

  /**
   * The animation.
   */
  private NodeAnimation animation;

  /**
   * The HTML used to generate the closed image.
   */
  private final SafeHtml closedImageHtml;

  /**
   * The HTML used to generate the closed image for the top items.
   */
  private final SafeHtml closedImageTopHtml;

  /**
   * The default number of children to display under each node.
   */
  private int defaultNodeSize = DEFAULT_LIST_SIZE;

  /**
   * The maximum width of the open and closed images.
   */
  private final int imageWidth;

  /**
   * Indicates whether or not animations are enabled.
   */
  private boolean isAnimationEnabled;

  /**
   * The {@link DragAndDropCellTreeNodeView} whose children are currently being selected
   * using the keyboard.
   */
  private DragAndDropCellTreeNodeView<?> keyboardSelectedNode;

  /**
   * The HTML used to generate the loading image.
   */
  private final SafeHtml loadingImageHtml;

  /**
   * The HTML used to generate the open image.
   */
  private final SafeHtml openImageHtml;

  /**
   * The HTML used to generate the open image for the top items.
   */
  private final SafeHtml openImageTopHtml;

  /**
   * The styles used by this widget.
   */
  private final Style style;

  private int tabIndex;

  /**
   * Construct a new {@link CellTree}.
   *
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   */
  public <T> DragAndDropCellTree(TreeViewModel viewModel, T rootValue) {
    this(viewModel, rootValue, getDefaultResources());
  }

  /**
   * Construct a new {@link CellTree}.
   *
   * @param <T> the type of data in the root node
   * @param viewModel the {@link TreeViewModel} that backs the tree
   * @param rootValue the hidden root value of the tree
   * @param resources the resources used to render the tree
   */
  public <T> DragAndDropCellTree(TreeViewModel viewModel, T rootValue, Resources resources) {
    super(viewModel);
    if (template == null) {
      template = GWT.create(Template.class);
    }
    if (TREE_IMPL == null) {
      TREE_IMPL = GWT.create(Impl.class);
    }
    this.style = resources.cellTreeStyle();
    this.style.ensureInjected();
    initWidget(new SimplePanel());
    setStyleName(this.style.cellTreeWidget());

    // Initialize the open and close images strings.
    ImageResource treeOpen = resources.cellTreeOpenItem();
    ImageResource treeClosed = resources.cellTreeClosedItem();
    ImageResource treeLoading = resources.cellTreeLoading();
    openImageHtml = getImageHtml(treeOpen, false);
    closedImageHtml = getImageHtml(treeClosed, false);
    openImageTopHtml = getImageHtml(treeOpen, true);
    closedImageTopHtml = getImageHtml(treeClosed, true);
    loadingImageHtml = getImageHtml(treeLoading, false);
    imageWidth = Math.max(Math.max(treeOpen.getWidth(), treeClosed.getWidth()),
        treeLoading.getWidth());

    // We use one animation for the entire tree.
    setAnimation(SlideAnimation.create());

    // Add event handlers.
    Set<String> eventTypes = new HashSet<String>();
    eventTypes.add("focus");
    eventTypes.add("blur");
    eventTypes.add("keydown");
    //eventTypes.add("mousedown");
    eventTypes.add("click");
    CellBasedWidgetImpl.get().sinkEvents(this, eventTypes);

    // Associate a view with the item.
    DragAndDropCellTreeNodeView<T> root = new DragAndDropCellTreeNodeView<T>(this, null, null,
        getElement(), rootValue);
    keyboardSelectedNode = rootNode = root;
    root.setOpen(true, false);
  }

  /**
   * Get the animation used to open and close nodes in this tree if animations
   * are enabled.
   *
   * @return the animation
   * @see #isAnimationEnabled()
   * @see #setAnimation(NodeAnimation)
   */
  public NodeAnimation getAnimation() {
    return animation;
  }

  /**
   * Get the default maximum number of children to display under each tree node.
   *
   * @return the default node size
   * @see #setDefaultNodeSize(int)
   */
  public int getDefaultNodeSize() {
    return defaultNodeSize;
  }

  @Override
  public TreeNode getRootTreeNode() {
    return rootNode.getTreeNode();
  }

  public int getTabIndex() {
    return tabIndex;
  }

  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  @Override
  public void onBrowserEvent(Event event) {
    CellBasedWidgetImpl.get().onBrowserEvent(this, event);
    if (isRefreshing) {
      // Ignore spurious events (onblur) while replacing elements.
      return;
    }
    super.onBrowserEvent(event);

    String eventType = event.getType();
    if ("focus".equals(eventType)) {
      // Remember the focus state.
      isFocused = true;
      onFocus();
    } else if ("blur".equals(eventType)) {
      // Remember the blur state.
      isFocused = false;
      onBlur();
    } else if ("keydown".equals(eventType) && !cellIsEditing) {
      int keyCode = event.getKeyCode();
      switch (keyCode) {
        // Handle keyboard navigation.
        case KeyCodes.KEY_DOWN:
        case KeyCodes.KEY_UP:
        case KeyCodes.KEY_RIGHT:
        case KeyCodes.KEY_LEFT:
          handleKeyNavigation(keyCode);

          // Prevent scrollbars from scrolling.
          event.preventDefault();
          return;
        case 32:
          // Handle space bar selection.
          if (KeyboardSelectionPolicy.ENABLED == getKeyboardSelectionPolicy()) {
            keyboardSelectedNode.setSelected(!keyboardSelectedNode.isSelected());

            // Prevent scrollbars from scrolling.
            event.preventDefault();
          }
          return;
      }
    }

    final Element target = event.getEventTarget().cast();
    ArrayList<Element> chain = new ArrayList<Element>();
    collectElementChain(chain, getElement(), target);

    final boolean isClick = "click".equals(eventType);
    final DragAndDropCellTreeNodeView<?> nodeView = findItemByChain(chain, 0, rootNode);
    if (nodeView != null && nodeView != rootNode) {
      if (isClick) {
        Element showMoreElem = nodeView.getShowMoreElement();
        if (nodeView.getImageElement().isOrHasChild(target)) {
          // Open the node when the open image is clicked.
          nodeView.setOpen(!nodeView.isOpen(), true);
          return;
        } else if (showMoreElem != null && showMoreElem.isOrHasChild(target)) {
          // Show more rows when clicked.
          nodeView.showMore();
          return;
        }
      }

      // Forward the event to the cell
      if (nodeView.getSelectionElement().isOrHasChild(target)) {
        // Move the keyboard focus to the clicked item.
        if (isClick) {
          /*
           * If the selected element is natively focusable, then we do not want to
           * steal focus away from it.
           */
          boolean isFocusable = CellBasedWidgetImpl.get().isFocusable(target);
          isFocused = isFocused || isFocusable;
          keyboardSelect(nodeView, !isFocusable);
        }

        nodeView.fireEventToCell(event);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * Setting the key to (int) 0 will disable the access key.
   * </p>
   * 
   * @see #getAccessKey()
   */
  public void setAccessKey(char key) {
    this.accessKey = key;
    keyboardSelectedNode.setKeyboardSelected(true, false);
  }

  /**
   * Set the animation used to open and close nodes in this tree. You must call
   * {@link #setAnimationEnabled(boolean)} to enable or disable animation.
   *
   * @param animation a {@link NodeAnimation}
   * @see #setAnimationEnabled(boolean)
   * @see #getAnimation()
   */
  public void setAnimation(NodeAnimation animation) {
    assert animation != null : "animation cannot be null";
    this.animation = animation;
  }

  public void setAnimationEnabled(boolean enable) {
    this.isAnimationEnabled = enable;
    if (!enable && animation != null) {
      animation.cancel();
    }
  }

  /**
   * Set the default number of children to display beneath each child node. If
   * more nodes are available, a button will appear at the end of the list
   * allowing the user to show more items. Changing this value will not affect
   * tree nodes that are already open.
   *
   * @param defaultNodeSize the max
   * @see #getDefaultNodeSize()
   */
  public void setDefaultNodeSize(int defaultNodeSize) {
    this.defaultNodeSize = defaultNodeSize;
  }

  public void setFocus(boolean focused) {
    keyboardSelectedNode.setKeyboardSelected(true, true);
  }

  public void setTabIndex(int index) {
    this.tabIndex = index;
    keyboardSelectedNode.setKeyboardSelected(true, false);
  }

  /**
   * Get the access key.
   *
   * @return the access key, or -1 if not set
   * @see #setAccessKey(char)
   */
  protected char getAccessKey() {
    return accessKey;
  }

  /**
   * Called when the keyboard selected node loses focus.
   */
  protected void onBlur() {
    keyboardSelectedNode.setKeyboardSelectedStyle(false);
  }

  /**
   * Called when the keyboard selected node gains focus.
   */
  protected void onFocus() {
    keyboardSelectedNode.setKeyboardSelectedStyle(true);
  }

  /**
   * Cancel a pending animation.
   */
  void cancelTreeNodeAnimation() {
    animation.cancel();
  }

  /**
   * Get the HTML to render the closed image.
   *
   * @param isTop true if the top element, false if not
   * @return the HTML string
   */
  SafeHtml getClosedImageHtml(boolean isTop) {
    return isTop ? closedImageTopHtml : closedImageHtml;
  }

  /**
   * Get the width required for the images.
   *
   * @return the maximum width required for images.
   */
  int getImageWidth() {
    return imageWidth;
  }

  /**
   * Return the node that has keyboard selection.
   */
  DragAndDropCellTreeNodeView<?> getKeyboardSelectedNode() {
    return keyboardSelectedNode;
  }

  /**
   * Return the HTML to render the loading image.
   */
  SafeHtml getLoadingImageHtml() {
    return loadingImageHtml;
  }

  /**
   * Get the HTML to render the open image.
   *
   * @param isTop true if the top element, false if not
   * @return the HTML string
   */
  SafeHtml getOpenImageHtml(boolean isTop) {
    return isTop ? openImageTopHtml : openImageHtml;
  }

  /**
   * Return the Style used by the tree.
   */
  public Style getStyle() {
    return style;
  }

  /**
   * Select a node using the keyboard.
   *
   * @param node the new node to select
   * @param stealFocus true to steal focus, false not to
   */
  void keyboardSelect(DragAndDropCellTreeNodeView<?> node, boolean stealFocus) {
    if (isKeyboardSelectionDisabled()) {
      return;
    }

    // Deselect the old node if it not destroyed.
    if (keyboardSelectedNode != null && !keyboardSelectedNode.isDestroyed()) {
      keyboardSelectedNode.setKeyboardSelected(false, false);
    }
    keyboardSelectedNode = node;
    keyboardSelectedNode.setKeyboardSelected(true, stealFocus);
  }

  /**
   * Animate the current state of a {@link DragAndDropCellTreeNodeView} in this tree.
   *
   * @param node the node to animate
   */
  void maybeAnimateTreeNode(DragAndDropCellTreeNodeView<?> node) {
    if (animation != null) {
      animation.animate(node, node.consumeAnimate() && isAnimationEnabled()
          && !node.isRootNode());
    }
  }

  /**
   * If this widget has focus, reset it.
   */
  void resetFocus() {
    CellBasedWidgetImpl.get().resetFocus(new Scheduler.ScheduledCommand() {
      public void execute() {
        if (isFocused && !keyboardSelectedNode.isDestroyed()
            && !keyboardSelectedNode.resetFocusOnCell()) {
          keyboardSelectedNode.setKeyboardSelected(true, true);
        }
      }
    });
  }

  /**
   * Collects parents going up the element tree, terminated at the tree root.
   */
  private void collectElementChain(ArrayList<Element> chain, Element hRoot,
      Element hElem) {
    if ((hElem == null) || (hElem == hRoot)) {
      return;
    }

    collectElementChain(chain, hRoot, hElem.getParentElement());
    chain.add(hElem);
  }

  private DragAndDropCellTreeNodeView<?> findItemByChain(ArrayList<Element> chain,
      int idx, DragAndDropCellTreeNodeView<?> parent) {
    if (idx == chain.size()) {
      return parent;
    }

    Element hCurElem = chain.get(idx);
    for (int i = 0, n = parent.getChildCount(); i < n; ++i) {
      DragAndDropCellTreeNodeView<?> child = parent.getChildNode(i);
      if (child.getElement() == hCurElem) {
        DragAndDropCellTreeNodeView<?> retItem = findItemByChain(chain, idx + 1, child);
        if (retItem == null) {
          return child;
        }
        return retItem;
      }
    }

    return findItemByChain(chain, idx + 1, parent);
  }

  /**
   * Get the HTML representation of an image.
   * 
   * @param res
   *          the {@link ImageResource} to render as HTML
   * @param isTop
   *          true if the image is for a top level element.
   * @return the rendered HTML
   */
  private SafeHtml getImageHtml(ImageResource res, boolean isTop) {
    StringBuilder classesBuilder = new StringBuilder(style.cellTreeItemImage());
    if (isTop) {
      classesBuilder.append(" ").append(style.cellTreeTopItemImage());
    }

    String direction;
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      direction = "right";
    } else {
      direction = "left";
    }

    AbstractImagePrototype proto = AbstractImagePrototype.create(res);
    SafeHtml image = SafeHtmlUtils.fromTrustedString(proto.getHTML());
    return TREE_IMPL.imageWrapper(classesBuilder.toString(), direction,
        res.getWidth(), res.getHeight(), image);
  }

  /**
   * Handle keyboard navigation.
   *
   * @param keyCode the key code that was pressed
   */
  private void handleKeyNavigation(int keyCode) {
    DragAndDropCellTreeNodeView<?> parent = keyboardSelectedNode.getParentNode();
    int parentChildCount = (parent == null) ? 0 : parent.getChildCount();
    int index = (parent == null) ? 0 : parent.indexOf(keyboardSelectedNode);
    int childCount = keyboardSelectedNode.getChildCount();

    switch (keyCode) {
      case KeyCodes.KEY_DOWN:
        if (keyboardSelectedNode.isOpen() && childCount > 0) {
          // Select first child.
          keyboardSelect(keyboardSelectedNode.getChildNode(0), true);
        } else if (index < parentChildCount - 1) {
          // Next sibling.
          keyboardSelect(parent.getChildNode(index + 1), true);
        } else {
          // Next available sibling of parent hierarchy.
          DragAndDropCellTreeNodeView<?> curParent = parent;
          DragAndDropCellTreeNodeView<?> nextSibling = null;
          while (curParent != null && curParent != rootNode) {
            DragAndDropCellTreeNodeView<?> grandparent = curParent.getParentNode();
            if (grandparent == null) {
              break;
            }
            int curParentIndex = grandparent.indexOf(curParent);
            if (curParentIndex < grandparent.getChildCount() - 1) {
              nextSibling = grandparent.getChildNode(curParentIndex + 1);
              break;
            }
            curParent = grandparent;
          }
          if (nextSibling != null) {
            keyboardSelect(nextSibling, true);
          }
        }
        break;
      case KeyCodes.KEY_UP:
        if (index > 0) {
          // Deepest node of previous sibling hierarchy.
          DragAndDropCellTreeNodeView<?> prevSibling = parent.getChildNode(index - 1);
          if (prevSibling.isOpen() && prevSibling.getChildCount() > 0) {
            prevSibling = prevSibling.getChildNode(prevSibling.getChildCount() - 1);
          }
          keyboardSelect(prevSibling, true);
        } else if (parent != null && parent != rootNode) {
          // Parent.
          keyboardSelect(parent, true);
        }
        break;
      case KeyCodes.KEY_RIGHT:
        if (LocaleInfo.getCurrentLocale().isRTL()) {
          keyboardNavigateShallow();
        } else {
          keyboardNavigateDeep();
        }
        break;
      case KeyCodes.KEY_LEFT:
        if (LocaleInfo.getCurrentLocale().isRTL()) {
          keyboardNavigateDeep();
        } else {
          keyboardNavigateShallow();
        }
        break;
    }
  }

  /**
   * Navigate to a deeper node. If the node is closed, open it. If it is open,
   * move to the first child.
   */
  private void keyboardNavigateDeep() {
    if (!keyboardSelectedNode.isLeaf()) {
      boolean isOpen = keyboardSelectedNode.isOpen();
      if (isOpen && keyboardSelectedNode.getChildCount() > 0) {
        // First child.
        keyboardSelect(keyboardSelectedNode.getChildNode(0), true);
      } else if (!isOpen) {
        // Open the node.
        keyboardSelectedNode.setOpen(true, true);
      }
    }
  }

  /**
   * Navigate to a shallower node. If the node is open, close it. If it is
   * closed, move to the parent.
   */
  private void keyboardNavigateShallow() {
    DragAndDropCellTreeNodeView<?> parent = keyboardSelectedNode.getParentNode();
    if (keyboardSelectedNode.isOpen()) {
      // Close the node.
      keyboardSelectedNode.setOpen(false, true);
    } else if (parent != null && parent != rootNode) {
      // Select the parent.
      keyboardSelect(parent, true);
    }
  }
  
  /*
   * Change for drag and drop 
   */
  
  public boolean isLeaf(Object value) {
    return super.isLeaf(value);
    
  }
  
  public <T> TreeViewModel.NodeInfo<?> getNodeInfo(T value) {
    return super.getNodeInfo(value);
  }
  

  public boolean isKeyboardSelectionDisabled() {
    return super.isKeyboardSelectionDisabled();
  }
  

  private EventBus dragAndDropHandlerManager;

  protected final <H extends EventHandler> HandlerRegistration addDragAndDropHandler(
      H handler, Type<H> type) {
    return ensureDragAndDropHandlers().addHandler(type, handler);
  }

  protected EventBus ensureDragAndDropHandlers() {

    return dragAndDropHandlerManager == null ? dragAndDropHandlerManager = new SimpleEventBus()
        : dragAndDropHandlerManager;
  }

  /**
   * Add a handler object that will manage the {@link BeforeDragStartEvent}
   * event. this kind of event is fired before the initialization of the drag
   * operation.
   */
  public HandlerRegistration addCellBeforeDragHandler(
      BeforeDragStartEventHandler handler) {
    return addDragAndDropHandler(handler, BeforeDragStartEvent.TYPE);
  }

  /**
   * Add a handler object that will manage the {@link DragEvent} event. this
   * kind of event is fired while a cell is being dragged
   */
  public HandlerRegistration addDragHandler(DragEventHandler handler) {
    return addDragAndDropHandler(handler, DragEvent.TYPE);
  }

  /**
   * Add a handler object that will manage the {@link DragStartEvent} event.
   * This kind of event is fired when the drag operation starts.
   */
  public HandlerRegistration addDragStartHandler(DragStartEventHandler handler) {
    return addDragAndDropHandler(handler, DragStartEvent.TYPE);
  }

  /**
   * Add a handler object that will manage the {@link DragStopEvent} event. This
   * kind of event is fired when the drag operation stops.
   */
  public HandlerRegistration addDragStopHandler(DragStopEventHandler handler) {
    return addDragAndDropHandler(handler, DragStopEvent.TYPE);
  }

  /**
   * Add a handler object that will manage the {@link ActivateDroppableEvent}
   * event. This kind of event is fired each time a droppable cell is activated.
   */
  public HandlerRegistration addActivateDroppableHandler(
      ActivateDroppableEventHandler handler) {
    return addDragAndDropHandler(handler, ActivateDroppableEvent.TYPE);
  }

  /**
   * Add a handler object that will manage the {@link DeactivateDroppableEvent}
   * event. This kind of event is fired each time a droppable cell is
   * deactivated.
   */
  public HandlerRegistration addDeactivateDroppableHandler(
      DeactivateDroppableEventHandler handler) {
    return addDragAndDropHandler(handler, DeactivateDroppableEvent.TYPE);
  }

  /**
   * Add a handler object that will manage the {@link DropEvent} event. This
   * kind of event is fired when an acceptable draggable is drop on a droppable
   * cell.
   */
  public HandlerRegistration addDropHandler(DropEventHandler handler) {
    return addDragAndDropHandler(handler, DropEvent.TYPE);
  }

  /**
   * Add a handler object that will manage the {@link OutDroppableEvent} event.
   * This kind of event is fired when an acceptable draggable is being dragged
   * out of a droppable cell.
   */
  public HandlerRegistration addOutDroppableHandler(
      OutDroppableEventHandler handler) {
    return addDragAndDropHandler(handler, OutDroppableEvent.TYPE);
  }

  /**
   * Add a handler object that will manage the {@link OverDroppableEvent} event.
   * This kind of event is fired when an acceptable draggable is being dragged
   * over a droppable cell.
   */
  public HandlerRegistration addOverDroppableHandler(
      OverDroppableEventHandler handler) {
    return addDragAndDropHandler(handler, OverDroppableEvent.TYPE);
  }

}
