import React from 'react';
import ReactDOM from 'react-dom';
import {BrowserRouter} from 'react-router-dom'
import {JssProvider} from 'react-jss'
import registerServiceWorker from './registerServiceWorker';
import {createMuiTheme, MuiThemeProvider} from '@material-ui/core/styles';

import Main from 'Main';
import CssBaseline from "@material-ui/core/CssBaseline";
import lightGreen from "@material-ui/core/colors/lightGreen";
import blueGrey from "@material-ui/core/colors/blueGrey";

const theme = createMuiTheme({
  palette: {
    primary: lightGreen,
    secondary: blueGrey,
  },
})

ReactDOM.render(
  <JssProvider>
    <React.Fragment>
      <CssBaseline/>
      <MuiThemeProvider theme={theme}>
        <BrowserRouter>
          <Main/>
        </BrowserRouter>
      </MuiThemeProvider>
    </React.Fragment>
  </JssProvider>,

  document.getElementById('root')
);
registerServiceWorker();
