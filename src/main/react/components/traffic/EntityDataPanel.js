import React, {Component} from "react";
import formatters from "common/Formatters"

import {EmptyDataView, ExceptionDataView, HexDataView, ImageDataView, PreFormattedDataView} from "./dataviews"

import Typography from "@material-ui/core/Typography";
import IconButton from "@material-ui/core/IconButton";
import Table from "@material-ui/core/Table";
import TableHead from "@material-ui/core/TableHead";
import FileDownloadIcon from "@material-ui/icons/FileDownload";
import TableRow from "@material-ui/core/TableRow";
import TableCell from "@material-ui/core/TableCell";
import TableBody from '@material-ui/core/TableBody';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';

import Select from '@material-ui/core/Select'
import FormControl from '@material-ui/core/FormControl'
import InputLabel from '@material-ui/core/InputLabel'
import MenuItem from '@material-ui/core/MenuItem'

function EntityHeadersTable({headers}) {
  return <Table>
    <TableHead>
      <TableRow>
        <TableCell>Key</TableCell>
        <TableCell>Values</TableCell>
      </TableRow>
    </TableHead>
    <TableBody>
      {Object.keys(headers).map(key => (
        <TableRow key={key}>
          <TableCell>{key}</TableCell>
          <TableCell>{headers[key].map((value, index) => <span key={`header-${key}-${index}`}><code>{value}</code></span>)}</TableCell>
        </TableRow>
      ))}
    </TableBody>
  </Table>
}

export default class EntityDataPanel extends Component {
  state = {
    tabIndex: 0,
    renderer: 'None'
  }

  renderers = [
    { name: 'XML', mimeType: ['application', 'xml'], renderer: text => <PreFormattedDataView>{formatters.xml(text)}</PreFormattedDataView> },
    { name: 'JSON', mimeType: ['application', 'json'], renderer: text => <PreFormattedDataView>{formatters.json(text)}</PreFormattedDataView> },
    { name: 'CSS', mimeType: ['text, css'], renderer: text => <PreFormattedDataView>{formatters.css(text)}</PreFormattedDataView> },
    { name: 'Text', mimeType: ['text', ''], renderer: text => <PreFormattedDataView>{text}</PreFormattedDataView>},
    { name: 'Image (256x256)', mimeType: ['image', ''], renderer: (_, href) => <ImageDataView src={href} width="256" height="256"/>},
    { name: 'Image (actual size)', mimeType: ['image', ''], renderer: (_, href) => <ImageDataView src={href}/>},
    { name: 'Binary', mimeType: ['', ''], renderer: data => <HexDataView data={data}/>},
    { name: 'None', mimeType: ['', ''], renderer: _ => <EmptyDataView/>},
  ]

  renderHttpHeaders = (headers) => {
    return headers
      ? <EntityHeadersTable headers={headers}/>
      : <Typography>No headers</Typography>
  }

  renderHttpBody = (id, kind, href, item, dataRenderer) => {
    if (item.exception) {
      return <div>
        <Typography variant="subheading">{kind} exception details:</Typography>
        <ExceptionDataView exception={item.exception}/>
      </div>
    } else if (item.data) {
      return <div>
        <FormControl style={{minWidth: '10rem'}}>
          <InputLabel htmlFor={`${kind}-dataformat-${id}`}>Display as</InputLabel>
          <Select id={`${kind}-dataformat-${id}`} label="Label" value={this.state.renderer}
                  onChange={e => this.setState({renderer: e.target.value})}>
            {this.renderers.map(renderer =>
              <MenuItem value={renderer.name} key={renderer.name}>
                {renderer.name}
              </MenuItem>
            )}
          </Select>
        </FormControl>
        {href &&
        <IconButton href={href} download={id + '-' + kind} title="Download"><FileDownloadIcon/></IconButton>}
        <div>
          {dataRenderer.renderer(item.data, href)}
        </div>
      </div>
    } else {
      return <Typography>Empty body</Typography>
    }
  }

  getDataRenderer(contentType) {
    const [type, subtype] = (contentType||'/').split('/')

    for (let renderer of this.renderers) {
      if (type.indexOf(renderer.mimeType[0]) >= 0 && subtype.indexOf(renderer.mimeType[1]) >= 0) {
        return renderer
      }
    }

    throw new Error("could not render content of type: " + contentType)
  }

  componentDidMount() {
    this.setState({renderer: this.getDataRenderer(this.props.item.contentType).name})
  }

  render() {
    const {id, kind, href, item} = this.props
    const dataRenderer = this.renderers.find(renderer => renderer.name === this.state.renderer)
    const tabs = [
      {
        label: `${kind} Data`, render: () => {
          return this.renderHttpBody(id, kind, href, item, dataRenderer)
        }
      },
      {
        label: `${kind} Headers (${Object.keys(item.headers || {}).length})`,
        render: () => this.renderHttpHeaders(item.headers)
      },
    ]

    return (<React.Fragment>
      <Tabs value={this.state.tabIndex}>
        {tabs.map((tab, index) =>
          <Tab key={index} label={tab.label} onClick={() => this.setState({tabIndex: index})}/>
        )}
      </Tabs>

      {tabs[this.state.tabIndex].render()}
    </React.Fragment>)
  }
}
