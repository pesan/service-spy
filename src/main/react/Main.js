import React, {Component} from 'react';
import {Link, Route} from 'react-router-dom'
import AppBar from '@material-ui/core/AppBar'
import Toolbar from '@material-ui/core/Toolbar'
import IconButton from '@material-ui/core/IconButton'
import Settings from '@material-ui/icons/Settings'
import Grid from '@material-ui/core/Grid'
import LogsView from "components/LogsView";
import ConfigurationView from "components/ConfigurationView";
import Typography from "@material-ui/core/Typography";
import Snackbar from "@material-ui/core/Snackbar";
import InfoIcon from '@material-ui/icons/Info';
import CallSplitIcon from '@material-ui/icons/CallSplit';
import IconLabel from 'components/IconLabel'
import './Main.css'

class Main extends Component {
  state = {
    message: undefined
  }

  onSnack = (message) => this.setState({message})

  render() {
    return (
      <div>
        <Snackbar
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'center'
          }}
          open={!!this.state.message}
          message={<IconLabel icon={<InfoIcon/>} label={this.state.message}/>}
          onClose={() => this.setState({message: undefined})}
          autoHideDuration={2600}
        />
        <AppBar position="static">
          <Toolbar>
            <Grid container alignItems="center">
              <Grid item xs={6}>
                <Link to="/" style={{textDecoration: 'none'}}>
                  <Typography variant="headline">
                    <IconLabel icon={<CallSplitIcon/>} label="Service Spy"/>
                  </Typography>
                </Link>
              </Grid>
              <Grid item xs={6} style={{textAlign: 'right'}}>
                <IconButton component={Link} to="/config"><Settings/></IconButton>
              </Grid>
            </Grid>
          </Toolbar>
        </AppBar>
        <Route exact path="/" render={() => <LogsView onSnack={this.onSnack}/>}/>
        <Route exact path="/config" render={() => <ConfigurationView onSnack={this.onSnack}/>}/>
      </div>
    );
  }
}

export default Main;
