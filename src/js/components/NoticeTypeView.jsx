import React from 'react';
import NoticeList from './NoticeList.jsx';
import {Category} from '../enums.js';

export default React.createClass({
  getDefaultProps() {
    return {
      category: null,
      notices: [],
      content: null
    };
  },

  render() {
    let {category, notices, content} = this.props;
    let titleEl, contentEl, listEl;
    let className = 'harja-noticelist harja-' + category + '-noticelist';

    if (category) {
      listEl = (
        <div className="row">
          <div className="medium-12 small-12 columns">
            <div className={className}>
              <NoticeList notices={notices} category={category}/>
            </div>
          </div>
        </div>
      );
    }

    if (content) {
      let imagesEl, bodyEl;

      // If either content or body text is missing, use the whole row.
      // Otherwise split the display area
      let className = 'medium-12 large-6 column';

      if (!content.images || !content.body) {
        className = 'medium-12 large-12 column';
      }

      if (content.images) {
        const imageList = (
          content.images.map( (url, index) =>
            <div className="harja-notice-image column row" key={index}>
              <img src={url} />
            </div>
          )
        );
        imagesEl = (
            <div className={className}>
              {imageList}
          </div>
        );
      }

      if (content.body) {
        bodyEl = (
          <div className={className}>
            <p>{content.body}</p>
          </div>
        );
      }

      contentEl = (
        <div className="row">
          {bodyEl}
          {imagesEl}
        </div>
      );

      titleEl = (
        <div className="harja-singlenotice-title">
          <div className="medium-12 row column text-center">
            <h3>{content.title}</h3>
            <p>{content.short}</p>
          </div>
        </div>
      );
    }

    return (
      <div>
        {titleEl}
        {contentEl}
        {listEl}
      </div>
    );
  }
});
