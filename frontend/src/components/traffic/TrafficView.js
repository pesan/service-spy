import React, {Component} from 'react';
import Typography from "@material-ui/core/Typography";
import IconButton from "@material-ui/core/IconButton";
import InfoIcon from "@material-ui/icons/Info"
import DeleteIcon from "@material-ui/icons/Delete"
import SwapVert from "@material-ui/icons/SwapVert"
import SyncProblemIcon from "@material-ui/icons/SyncProblem"
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import FormControl from "@material-ui/core/FormControl";
import Grid from "@material-ui/core/Grid";
import EntityDataPanel from './EntityDataPanel';
import IconLabel from "../IconLabel";
import SearchInput from "../SearchInput"
import {DateTime} from "luxon"

import {delay, retryWhen, tap} from "rxjs/operators"

import {fromEventSource} from "service/ObservableSources"

export default class TrafficView extends Component {
  state = {
    message: undefined,
    filter: '',
    actions: [],
    online: false,
  }

  actionFilter = action => {
    const filter = this.state.filter.toLowerCase()
    if (!filter) {
      return true
    }

    return [
      action.request.requestPath,
      action.request.query,
      action.request.httpMethod,
      action.request.time,
      action.response.status,
      action.response.time,
      action.request.data,
      action.response.data,
    ].some(sourceText => (sourceText || '').toString().toLowerCase().indexOf(filter) >= 0)
  }

  componentDidMount() {
    this.setState({online: true, actions: []})
    this.subscription = fromEventSource('/api/traffic')
      .pipe(
        retryWhen(errors => errors // Error flow
          .pipe(
            tap(() => {
                this.setState({online: false})
                this.props.onSnack(<span style={{display: 'flex', alignItems: 'center'}}>
                  Connection lost. Retrying...
                </span>)
              }
            ),

            delay(10000),

            tap(() => this.setState({online: true, actions: []}))
          ))
      )
      .subscribe(event => {
        this.setState(({actions}) => ({
          actions: [
            ...actions,
            this.parseAction(JSON.parse(event.data))
          ]
        }))
      })
  }

  componentWillUnmount() {
    this.subscription.unsubscribe()
  }

  parseUTCDateTimeString(text) {
    return DateTime.fromISO(text, {zone: 'UTC'}).setZone("local")
  }

  parseAction(action) {
    return {
      ...action,
      request: {
        ...action.request,
        time: this.parseUTCDateTimeString(action.request.time),
        data: action.request.data && atob(action.request.data)
      },
      response: {
        ...action.response,
        time: this.parseUTCDateTimeString(action.response.time),
        data: action.response.data && atob(action.response.data)
      },
      expanded: false
    }
  }

  render() {
    const filteredActions = this.state.actions.filter(this.actionFilter)
    const connectionStatus = `Connection status: ${this.state.online ? 'In sync' : 'Offline'}`

    return (
      <div>
        <Typography variant="headline">
          Traffic
        </Typography>

        <Grid container style={{backgroundColor: '#fff', padding: '1em', marginBottom: '1em', borderRadius: 1, boxShadow: '1px 1px 2px gray', border: '1px solid #ddd'}}>
          <Grid item xs={6} style={{display: 'flex', alignItems: 'center'}}>
            <IconButton title={connectionStatus} onClick={() =>
              this.props.onSnack(connectionStatus)
            }>
            {this.state.online ? <SwapVert/> : <SyncProblemIcon/>}
            </IconButton>

            <IconButton title="Clear actions" onClick={() => {
              this.setState({actions: []});
              this.props.onSnack("Actions cleared")
            }}>
              <DeleteIcon/>
            </IconButton>
          </Grid>
          <Grid xs={6} item style={{textAlign: 'right'}}>
            <form>
              <FormControl>
                <SearchInput value={this.state.filter} onChange={filter => this.setState({filter})}/>
              </FormControl>
            </form>
            <Typography style={{color: 'gray'}}>Displaying {filteredActions.length} of {this.state.actions.length} action(s)</Typography>
          </Grid>
        </Grid>

        {!this.state.actions.length &&
        <Typography style={{width: "100%", marginTop: 80, textAlign: 'center'}} variant="title">
          <IconLabel icon={<InfoIcon/>} label="No actions to display"/>
        </Typography>
        }

        {filteredActions.map(action => <EntityItem key={action.id} action={action} online={this.state.online} expanded={true}/>)}
      </div>
    );
  }
}

class EntityItem extends Component {
  constructor(props) {
    super(props)
    this.state = {
      expanded: !!props.expanded
    }
  }

  render() {
    const {action, online} = this.props
    return <ExpansionPanel disabled={!online} expanded={this.state.expanded} onChange={(e, expanded) => this.setState(({expanded}))}>
      <ExpansionPanelSummary expandIcon={<ExpandMoreIcon/>}>
        <Grid container spacing={16}>
          <Grid item xs={3}>
            <Typography title={action.request.time.zoneName}>
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
        <EntityDataPanels>
          <EntityDataPanel key="request-data-panel" id={action.id} kind="Request" href={action.href.requestData} item={action.request}/>
          <EntityDataPanel key="response-data-panel" id={action.id} kind="Response" href={action.href.responseData} item={action.response}/>
        </EntityDataPanels>
      </ExpansionPanelDetails>
    </ExpansionPanel>
  }
}

class EntityDataPanels extends Component {
  render() {
    return <Grid container>
      {this.props.children.map(child => <Grid item key={child.key} style={{overflow: 'auto'}} xs={6}>{child}</Grid>)}
    </Grid>
  }
}