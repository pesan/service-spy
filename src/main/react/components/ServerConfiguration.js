import React, {Component} from 'react';
import TableRow from "@material-ui/core/TableRow";
import TableCell from "@material-ui/core/TableCell";
import IconLabel from "components/IconLabel";
import IconButton from "@material-ui/core/IconButton";
import ExpansionPanel from "@material-ui/core/ExpansionPanel";
import ExpansionPanelSummary from "@material-ui/core/ExpansionPanelSummary";
import Typography from "@material-ui/core/Typography";
import DeviceHubIcon from '@material-ui/icons/DeviceHub'
import AddCircleOutlineIcon from '@material-ui/icons/AddCircleOutline'
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableHead from '@material-ui/core/TableHead';
import Button from '@material-ui/core/Button';
import InfoIcon from '@material-ui/icons/Info';
import ArrowUpwardIcon from '@material-ui/icons/ArrowUpward';
import ArrowDownwardIcon from '@material-ui/icons/ArrowDownward';
import ContentCopyIcon from '@material-ui/icons/ContentCopy';
import DeleteIcon from '@material-ui/icons/Delete';
import Switch from '@material-ui/core/Switch';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import TextField from "@material-ui/core/TextField";

class ServerConfiguration extends Component {

  noMappings = () => (
    <TableRow>
      <TableCell colSpan="4" className="text-center text-muted">
        <IconLabel icon={<InfoIcon/>} label="No mappings defined."/>
      </TableCell>
    </TableRow>
  )

  renderMappings = (server, mapping, index, handlers) => (
    <TableRow key={`${server.name}-mapping-${index}`}>
      <TableCell>
        <Switch color="primary" checked={mapping.active} onChange={handlers.onChangeActive}/>
      </TableCell>

      <TableCell>
        <TextField value={mapping.pattern} onChange={handlers.onChangePattern}/>
      </TableCell>

      <TableCell>
        <TextField value={mapping.url} onChange={handlers.onChangeUrl}/>
      </TableCell>

      <TableCell>
        <IconButton disableRipple disabled={!handlers.onMoveUp} title="Move up" onClick={handlers.onMoveUp}><ArrowUpwardIcon/></IconButton>
        <IconButton disableRipple disabled={!handlers.onMoveDown} title="Move down" onClick={handlers.onMoveDown}><ArrowDownwardIcon/></IconButton>
        <IconButton disabled={!handlers.onDuplicate} title="Duplicate" onClick={handlers.onDuplicate}><ContentCopyIcon/></IconButton>
        <IconButton disabled={!handlers.onRemove} title="Remove" onClick={handlers.onRemove}> <DeleteIcon/> </IconButton>
      </TableCell>

    </TableRow>
  )

  render() {
    const {server, serverMappings, onAdd, handlers} = this.props

    return <ExpansionPanel key={server.name}>
      <ExpansionPanelSummary expandIcon={<ExpandMoreIcon/>}>
        <Typography>
          <IconLabel icon={<DeviceHubIcon/>} label={
            <span>Server <strong>{server.name}</strong> listening on <code>{server.host}:{server.port}</code> {server.ssl && '(SSL)'}</span>
          }/>
        </Typography>
      </ExpansionPanelSummary>
      <ExpansionPanelDetails>

        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Active</TableCell>
              <TableCell>Pattern (regexp)</TableCell>
              <TableCell>URL</TableCell>
              <TableCell>
                <Button disabled={!onAdd} color="primary" title="Add mapping" onClick={onAdd}> <AddCircleOutlineIcon/> Add</Button>
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {
              serverMappings.length
                ? serverMappings.map((mapping, index) => this.renderMappings(server, mapping, index, handlers(index)))
                : this.noMappings()
            }
          </TableBody>
        </Table>
      </ExpansionPanelDetails>
    </ExpansionPanel>
  }
}

export default ServerConfiguration;