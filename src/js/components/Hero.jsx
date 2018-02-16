import React from 'react';
import NavButton from './NavButton.jsx'
import DevLinks from './DevLinks.jsx';
import News from './News.jsx';
import {Events, Category} from '../enums.js';

export default React.createClass({
  render() {

    const linkCare = {title: "Teiden hoito", data: {action: Events.CATEGORY, category: Category.CARE}};
    const moreCareEl = (
      <div className="harja-more harja-care">
        <NavButton item={linkCare} />
      </div>
    );

    const linkMaintenance = {title: "Teiden ylläpito", data: {action: Events.CATEGORY, category: Category.MAINTENANCE}};
    const moreMaintenanceEl = (
      <div className="harja-more harja-maintenance">
        <NavButton item={linkMaintenance} />
      </div>
    );

    const linkFaq = {title: "Koulutusvideot", data: {action: Events.CATEGORY, category: Category.FAQ}};
    const moreFaqEl = (
      <div className="harja-more harja-faq">
        <NavButton item={linkFaq} />
      </div>
    );

    const linkWater = {title: "Vesiväylät", data: {action: Events.CATEGORY, category: Category.WATERWAYS}};
    const moreWaterEl = (
      <div className="harja-more harja-waterways">
        <NavButton item={linkWater} />
      </div>
    );
    
    
    const linkProblemsolving = {title: "Ongelmanratkaisu", data: {action: Events.CATEGORY, category: Category.PROBLEMSOLVING}};
    const moreWaterEl = (
      <div className="harja-more harja-problemsolving">
        <NavButton item={linkProblemsolving} />
      </div>
    );

    return (
      <div className="harja-hero">
      <section className="harja-hero-large show-for-large">
        <div className="row column align-middle text-center">
          <News news={this.props.news}/>
          <div className="row">
            <div className="large-4 column">
              {moreCareEl}
            </div>
            <div className="large-4 column">
              {moreMaintenanceEl}
            </div>
            <div className="large-4 column">
              {moreWaterEl}
            </div>
          </div>
        </div>
        <DevLinks />
      </section>
      <section className="harja-hero-small hide-for-large">
        <News news={this.props.news}/>
      </section>
      </div>
    );
  }
});
