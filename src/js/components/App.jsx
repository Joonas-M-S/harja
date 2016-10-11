import React from 'react';
import ReactDOM from 'react-dom';
import request from 'superagent';
import Home from './Home.jsx';
import {Category} from '../enums.js';

export default React.createClass({

  getInitialState() {
    let initialState = {
      Category: {}
    }
    initialState[Category.CARE] = []
    initialState[Category.MAINTENANCE] = []
    initialState[Category.FAQ] = []
    initialState[Category.CONTENT] = []
    return initialState;
  },

  componentDidMount() {
    const url = document.URL;
    const param = url.substr(url.lastIndexOf('?')+1,url.length);
    let test = '';
    if (param === 'test') test = 'test/';
    const careUrl = test + 'care.json';
    const maintenanceUrl = test + 'maintenance.json';
    const faqUrl = test + 'faq.json';

    if (param === 'test') {
      // Slow down fetching for development
      setTimeout(() => { this.getNotices(careUrl, Category.CARE); }, 500);
      setTimeout(() => { this.getNotices(maintenanceUrl, Category.MAINTENANCE); }, 3000);
      setTimeout(() => { this.getNotices(faqUrl, Category.FAQ); }, 5000);
      setTimeout(() => { this.getContent(); }, 5000);
    }
    else {
      this.getNotices(careUrl, Category.CARE);
      this.getNotices(maintenanceUrl, Category.MAINTENANCE);
      this.getNotices(faqUrl, Category.FAQ);
      this.getContent();
    }
  },

  defaultTitle(type) {
    switch(type) {
      case Category.CARE:
        return 'Hoitotiedote';
      case Category.MAINTENANCE:
        return 'Ylläpitötiedote';
      case Category.FAQ:
        return 'Koulutusvideo';
      default:
        return 'Tiedote';
    }
  },

  getNotices(file, type) {
    const url = 'data/' + file;
    request.get(url)
      .set('Accept', 'application/json')
      .end((err, response) => {
        if (err) return console.error(err);

        // 1. Create date from string
        // 2. Sort notices by date. Those with no date to bottom
        // 3. Add running index number and stringify date
        const notices = response.body.map((notice) => {
          // This way works on iphone and others
          let d = null;
          if (notice.date) {
            var arr = notice.date.split(/[-]/);
            if (arr.length >= 3) {
              d = new Date(arr[0], arr[1]-1, arr[2]);
              if (isNaN( d.getTime() )) {
                d = null;
              }
            }
          }
          notice.date = d;
          return notice;
        })
        .sort((a,b) => {
          if (a.date === null && b.date === null) return 0;
          if (a.date === null) return 1;
          if (b.date === null) return -1;
          return b.date.getTime() - a.date.getTime()
        })
        .map((notice, index) => {
          notice.id = index;
          notice.type = type
          notice.date = notice.date === null ? 'Ei päivämäärää' : notice.date.toLocaleDateString('fi-FI');
          notice.title = notice.title || this.defaultTitle(type);
          notice.body = notice.body || '';
          notice.images = notice.images || [];
          return notice;
        });

        this.setState({
          [type]: notices,
        });
      });
  },

  getContent() {
    const url = 'data/content.json';
    request.get(url)
      .set('Accept', 'application/json')
      .end((err, response) => {
        if (err) return console.error(err);

        const contents = response.body.map((content) => {
            content.title = content.title || '';
            content.short = content.short || null;
            content.body = content.body || null;
            content.images = content.images || null;
            if (content.images && content.images.length < 1) content.images = null;
            return content;
          })


        this.setState({
          [Category.CONTENT]: contents,
        });
      });
  },

  render () {


    return (
      <Home {...this.state}/>
    )
  }
});
