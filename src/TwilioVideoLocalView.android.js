/**
 * Component for Twilio Video local views.
 *
 * Authors:
 *   Jonathan Chang <slycoder@gmail.com>
 */

import { requireNativeComponent, View } from 'react-native'
import React from 'react'
import PropTypes from 'prop-types'

class TwilioVideoPreview extends React.Component {
  static propTypes = {
    ...View.propTypes,
    // Whether to apply Z ordering to this view.  Setting this to true will cause
    // this view to appear above other Twilio Video views.
   applyZOrder: PropTypes.bool,
    /**
    * How the video stream should be scaled to fit its
     * container.
     */
    scaleType: PropTypes.oneOf(['fit', 'fill']),
    onFrameDimensionsChanged: PropTypes.func,
  }

  buildNativeEventWrappers () {
    return [
      'onFrameDimensionsChanged'
    ].reduce((wrappedEvents, eventName) => {
      if (this.props[eventName]) {
        return {
          ...wrappedEvents,
          [eventName]: data => this.props[eventName](data.nativeEvent)
        }
      }
      return wrappedEvents
    }, {})
  }

  render () {
    return <NativeTwilioVideoPreview {...this.props} {...this.buildNativeEventWrappers()} />
  }
}

const NativeTwilioVideoPreview = requireNativeComponent(
  'RNTwilioVideoPreview',
  TwilioVideoPreview
)

module.exports = TwilioVideoPreview
