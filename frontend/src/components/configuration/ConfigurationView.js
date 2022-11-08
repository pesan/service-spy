import React, {Component} from 'react';
import IconButton from '@material-ui/core/IconButton';
import ArrowBackIcon from '@material-ui/icons/ArrowBack';
import ServerConfiguration from 'components/configuration/ServerConfiguration';
import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';
import SaveIcon from '@material-ui/icons/Save';
import RestoreIcon from '@material-ui/icons/Restore';
import Grid from "@material-ui/core/Grid";
import IconLabel from "../IconLabel";

class ConfigurationView extends Component {
  state = {
    servers: [],
    mappings: {}
  }

  onReload = () => {
    this.loadConfig()
  }

  componentDidMount() {
    this.loadConfig()
  }

  loadConfig() {
    fetch('/api/config')
      .then(response => response.json())
      .then(config => {
        this.setState({
          servers: Object.keys(config.servers).map(serverName => ({
            name: serverName,
            host: config.servers[serverName].host,
            port: config.servers[serverName].port,
            ssl: config.servers[serverName].ssl,
          })),
          mappings: Object.keys(config.servers).map(serverName => ({
            [serverName]: config.servers[serverName].mappings
          })).reduce((a, b) => {
            return {...a, ...b}
          }, {})
        })
      })
  }

  onAdd = (server) => () => {
    this.setState(current => ({
      mappings: {
        ...current.mappings,
        [server.name]: [...current.mappings[server.name], {url: '', pattern: '/.*', active: true}]
      }
    }))
  }

  onSave = () => {
    fetch('/api/config', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json; charset=utf-8'
      },
      body: JSON.stringify({
        mappings: this.state.servers.map(server => ({
          [server.name]: this.state.mappings[server.name]
        })).reduce((a, b) => ({...a, ...b}), {})
      })
    })
      .then(response => { if (!response.ok) throw Error(response.statusText) })
      .then(() => this.props.onSnack('Configuration saved'))
      .catch(err => this.props.onSnack('Failed to save configuration'))
      .then(() => this.loadConfig())
  }

  render() {
    const handlers = (server, serverMappingsCount) => (index) => {
      const setProperty = (n, f) => event => {
        const v = f(event)
        this.setState(current => ({
          mappings: {
            ...current.mappings,
            [server.name]: current.mappings[server.name].map((mapping, mappingsIndex) => mappingsIndex === index
              ? {...mapping, [n]: v}
              : mapping
            )
          }
        }))
      }
      return {
        onChangeActive: setProperty('active', e => e.target.checked),
        onChangeUrl: setProperty('url', e => e.target.value),
        onChangePattern: setProperty('pattern', e => e.target.value),
        onMoveUp: index > 0 ? () => {
          this.setState(current => ({
            mappings: {
              ...current.mappings,
              [server.name]: [
                ...current.mappings[server.name].slice(0, index - 1),
                current.mappings[server.name][index],
                current.mappings[server.name][index - 1],
                ...current.mappings[server.name].slice(index + 1)
              ]
            }
          }))
        } : undefined,
        onMoveDown: index < serverMappingsCount - 1 ? () => {
          this.setState(current => ({
            mappings: {
              ...current.mappings,
              [server.name]: [
                ...current.mappings[server.name].slice(0, index),
                current.mappings[server.name][index + 1],
                current.mappings[server.name][index],
                ...current.mappings[server.name].slice(index + 2)
              ]
            }
          }))
        } : undefined,
        onRemove: () => {
          this.setState(current => {
            return {
              mappings: {
                ...current.mappings,
                [server.name]: current.mappings[server.name].filter((_, i) => i !== index)
              }
            }
          })
        },
        onDuplicate: () => {
          this.setState(current => ({
            mappings: {
              ...current.mappings,
              [server.name]: [
                ...current.mappings[server.name].slice(0, index),
                {...current.mappings[server.name][index]},
                ...current.mappings[server.name].slice(index)
              ]
            }
          }))
        }
      }
    }

    return (
      <div>
        <Grid container>
          <Grid item xs={8}>
            <Typography variant="title">
              <IconLabel icon={
                <IconButton href="/"><ArrowBackIcon/></IconButton>
              } label="Configuration"/>
            </Typography>
          </Grid>
          <Grid item xs={4} style={{marginTop: '1em', textAlign: 'right'}}>
            <Button disabled={!this.onReload} variant="outlined" color="secondary"
                    onClick={this.onReload}><RestoreIcon/> Reset</Button>
            <Button disabled={!this.onSave} variant="outlined" color="primary"
                    onClick={this.onSave}><SaveIcon/> Save</Button>
          </Grid>
        </Grid>
        <Typography>
          Configurations for the available servers. Each server has pattern mappings, listed in the order
          of evaluation. The server will redirect requests for a given request path
          matching <strong>Pattern</strong> to <strong>URL</strong>.
        </Typography>
        {
          this.state.servers.map(server =>
            <ServerConfiguration
              key={server.name}
              server={server}
              serverMappings={this.state.mappings[server.name]}
              onAdd={this.onAdd(server)}
              handlers={handlers(server, this.state.mappings[server.name].length)}/>
          )
        }
      </div>
    );
  }
}

export default ConfigurationView;