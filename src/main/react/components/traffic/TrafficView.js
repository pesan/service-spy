import React, {Component} from 'react';
import Typography from "@material-ui/core/Typography";
import IconButton from "@material-ui/core/IconButton";
import InfoIcon from "@material-ui/icons/Info"
import CloseIcon from "@material-ui/icons/Close"
import SearchIcon from "@material-ui/icons/Search"
import DeleteIcon from "@material-ui/icons/Delete"
import SyncIcon from "@material-ui/icons/Sync"
import SyncProblemIcon from "@material-ui/icons/SyncProblem"
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import FormControl from "@material-ui/core/FormControl";
import Input from "@material-ui/core/Input";
import InputAdornment from "@material-ui/core/InputAdornment";
import Grid from "@material-ui/core/Grid";
import EntityDataPanel from './EntityDataPanel';
import IconLabel from "../IconLabel";
import {DateTime} from "luxon"

class TrafficView extends Component {
  state = {
    message: undefined,
    filter: '',
    actions: [],
    online: false,
  }

  evtSource = undefined
  retryTimerId = undefined

  actionFilter = action => {
    const filter = this.state.filter
    if (!filter) {
      return true
    }
    return (action.request.data && action.request.data.indexOf(filter) >= 0)
      || (action.response.data && action.response.data.indexOf(filter) >= 0)
  }

  componentDidMount() {
    this.makeConnection()
  }

  makeConnection = () => {
    this.retryTimerId && clearTimeout(this.retryTimerId)

    if (this.evtSource) {
      this.evtSource.onerror = undefined
      this.evtSource.close()
    }
    this.evtSource = new EventSource('/api/actions')

    // Assume online until we got an 'onerror' callback
    this.setState({online: true})

    this.evtSource.onopen = () => {
      this.setState({actions: [], online: true})
    }

    this.evtSource.onmessage = (event) => {
      this.setState(({actions}) => ({
        actions: [
          ...actions,
          this.parseAction(JSON.parse(event.data))
        ]
      }))
    }

    this.evtSource.onerror = () => {
      this.setState({online: false})
      this.retryTimerId = setTimeout(this.makeConnection, 10000)
      this.props.onSnack(<span style={{display: 'flex', alignItems: 'center'}}>Connection not established. Retrying...</span>)
    }
  }

  componentWillUnmount() {
    this.retryTimerId && clearTimeout(this.retryTimerId)
  }

  parseAction(action) {
    return {
      ...action,
      request: {
        ...action.request,
        time: DateTime.fromISO(action.request.time),
        data: action.request.data && atob(action.request.data)
      },
      response: {
        ...action.response,
        time: DateTime.fromISO(action.response.time),
        data: action.response.data && atob(action.response.data)
      }
    }
  }

  render() {
    const filteredActions = this.state.actions
      .filter(this.actionFilter)

    return (
      <div>

        <Typography variant="headline">
          Traffic
        </Typography>

        <Grid container>
          <Grid item xs={6}>
            { this.state.online
             ? <IconButton title="Online: syncing..."><SyncIcon/></IconButton>
             : <IconButton title="Offline" onClick={this.makeConnection}><SyncProblemIcon/></IconButton>
            }

            <IconButton onClick={() => {
              this.setState({actions: []});
              this.props.onSnack("List cleared")
            }}><DeleteIcon/></IconButton>
          </Grid>
          <Grid xs={6} item style={{textAlign: 'right'}}>
            <form>
              <FormControl>
                <Input
                  placeholder='Filter...'
                  value={this.state.filter}
                  onChange={e => this.setState({filter: e.target.value})}
                  startAdornment={<InputAdornment position="start"><SearchIcon/></InputAdornment>}
                  endAdornment={<InputAdornment position="end">
                    <IconButton onClick={() => this.setState({filter: ''})}>
                      <CloseIcon style={{fontSize: 14}}/>
                    </IconButton>
                  </InputAdornment>}
                />
              </FormControl>
            </form>
            <Typography>Displaying {filteredActions.length} of {this.state.actions.length} action(s)</Typography>
          </Grid>
        </Grid>

        {!this.state.actions.length &&
        <Typography style={{width: "100%", marginTop: 80, textAlign: 'center'}} variant="title">
          <IconLabel icon={<InfoIcon/>} label="No actions to display"/>
        </Typography>
        }

        {filteredActions.map(action => (
          <ExpansionPanel key={'ExtensionPanel-' + action.id} disabled={!this.state.online}>
            <ExpansionPanelSummary expandIcon={<ExpandMoreIcon/>}>
              <Grid container spacing={16}>
                <Grid item xs={3}>
                  <Typography>
                    {action.request.time.toFormat("yyyy-MM-dd HH:mm:ss.SSS")}
                  </Typography>
                </Grid>

                <Grid item xs={4} title={action.request.requestPath + (action.request.query || '')}>
                  <Typography>
                    <code>{action.request.httpMethod}</code>&nbsp;<code>{action.request.requestPath}
                    {action.request.query ? '?...' : ''}</code>
                  </Typography>
                </Grid>
                <Grid item xs={1}>
                  {(action.request.exception || action.response.exception)
                    ? <Typography color="error">ERROR</Typography>
                    : <Typography>{action.response.status}</Typography>
                  }
                </Grid>
                <Grid item xs={2}>
                  <Typography>
                    ({action.responseTimeMillis}&nbsp;ms)
                  </Typography>
                </Grid>
              </Grid>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails>
              <Grid container>
                <EntityDataPanel key="request-data-panel" id={action.id} kind="Request" href={action.href.requestData} item={action.request}/>
                <EntityDataPanel key="response-data-panel" id={action.id} kind="Response" href={action.href.responseData} item={action.response}/>
              </Grid>
            </ExpansionPanelDetails>
          </ExpansionPanel>
        ))
        }
      </div>
    );
  }
}

export default TrafficView;