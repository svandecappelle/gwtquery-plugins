/*
 * Copyright 2011, The gwtquery team.
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
package gwtquery.plugins.enhance.client.gwt;

import static com.google.gwt.query.client.GQuery.$;

import com.google.gwt.dom.client.Element;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.query.client.plugins.widgets.WidgetFactory;
import com.google.gwt.query.client.plugins.widgets.WidgetsUtils;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;

/**
 * Factory used to create a {@link SuggestBox} widget.
 * 
 */
public class SuggestBoxWidgetFactory implements WidgetFactory<SuggestBox> {

  /**
   * Options used to create a {@link SuggestBox}
   * 
   */
  public static class SuggestBoxOptions {

    private String suggestionsSelector;
    private SuggestOracle suggestOracle;

    public SuggestBoxOptions() {
      initDefault();
    }

    public SuggestBoxOptions(String suggestionsSelector) {
      this.suggestionsSelector = suggestionsSelector;
    }

    public SuggestBoxOptions(SuggestOracle suggestOracle) {
      this.suggestOracle = suggestOracle;
    }

    public SuggestOracle getSuggestOracle() {
      return suggestOracle;
    }

    public String getSuggestionsSelector() {
      return suggestionsSelector;
    }

    /**
     * Set the css selector to use for selecting elements playing roles of the
     * list items. The value of the items will be the inner texts of the
     * selected elements
     * 
     * If this options is null, the inner text of the direct children of the
     * element will be used as suggestions list.
     * 
     * This options is used if the element is not a <i>select</i> element.
     * 
     * @param optionsSelector
     */
    public void setSuggestionsSelector(String suggestionsSelector) {
      this.suggestionsSelector = suggestionsSelector;
    }

    public void setSuggestOracle(SuggestOracle suggestOracle) {
      this.suggestOracle = suggestOracle;
    }

    private void initDefault() {
      suggestionsSelector = null;
      suggestOracle = null;
    }
  }

  private SuggestBoxOptions options;

  public SuggestBoxWidgetFactory(SuggestBoxOptions options) {
    this.options = options;
  }

  public SuggestBox create(Element e) {

    SuggestOracle suggestOracle = createOracle(e);
 
    if ($(e).filter("input[type='text']").length() > 0) {
      return SuggestBox.wrap(suggestOracle, e);
    }

    SuggestBox sbox = new SuggestBox(suggestOracle);
    WidgetsUtils.replaceOrAppend(e, sbox);

    return sbox;
  }

  private SuggestOracle createOracle(Element e) {
    if (options.getSuggestOracle() != null) {
      return options.getSuggestOracle();
    }

    MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();

    if (options.getSuggestionsSelector() != null) {
      GQuery suggestions = $(options.getSuggestionsSelector(), e);
      for (Element suggestion : suggestions.elements()) {
        oracle.add(suggestion.getInnerText());
      }
    }

    return oracle;
  }

}